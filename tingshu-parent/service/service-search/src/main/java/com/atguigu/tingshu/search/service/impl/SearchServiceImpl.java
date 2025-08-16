package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.pinyin.engine.pinyin4j.Pinyin4jEngine;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.Repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.Repository.SuggestIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.micrometer.common.util.StringUtils;
import io.netty.util.internal.StringUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {

    private static final String INDEX_NAME = "albuminfo";

    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private AlbumInfoIndexRepository repository;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private ElasticsearchClient elasticsearchClient;
    @Autowired
    private SuggestIndexRepository suggestIndexRepository;
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;
    /**
     * 上架
     *
     * @param albumId
     */
    @Override
    public void upperAlbum(Long albumId) {
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();

        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑不存在，专辑ID{}", albumId);
            BeanUtils.copyProperties(albumInfo, albumInfoIndex);
            //2.2 处理专辑标签列表 A
            List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
            if (CollectionUtil.isNotEmpty(albumAttributeValueVoList)) {
                List<AttributeValueIndex> attributeValueIndexList = albumAttributeValueVoList.stream().map(albumAttributeValue -> {
                    //将专辑标签集合泛型从AlbumAttributeValue转为AttributeValueIndex
                    AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                    attributeValueIndex.setAttributeId(albumAttributeValue.getAttributeId());
                    attributeValueIndex.setValueId(albumAttributeValue.getValueId());
                    return attributeValueIndex;
                }).collect(Collectors.toList());
                albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
            }
            return albumInfo;
        }, threadPoolExecutor);

        CompletableFuture<Void> categoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //封装分类信息
            BaseCategoryView baseCategoryView = albumFeignClient.getbaseCategoryViewById(albumInfo.getCategory3Id()).getData();
            Assert.notNull(baseCategoryView, "分类信息不存在，分类ID{}", albumInfo.getCategory3Id());
            albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
            albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());
            albumInfoIndex.setCategory3Id(baseCategoryView.getCategory3Id());
        }, threadPoolExecutor);

        CompletableFuture<Void> userCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //封装播音员信息
            UserInfo userInfo = userFeignClient.getUserInfoById(albumInfo.getUserId()).getData();
            Assert.notNull(userInfo, "用户信息不存在，用户ID{}", albumInfo.getUserId());
            albumInfoIndex.setAnnouncerName(userInfo.getNickname());
        }, threadPoolExecutor);

        CompletableFuture<Void> stateCompletableFuture = CompletableFuture.runAsync(() -> {
            //5.TODO 封装统计信息，采用产生随机值 以及专辑热度
            //5.1 随机为专辑产生播放量，订阅量，购买量，评论量 、
            int num1 = RandomUtil.randomInt(1000, 2000);
            int num2 = RandomUtil.randomInt(500, 1000);
            int num3 = RandomUtil.randomInt(200, 400);
            int num4 = RandomUtil.randomInt(100, 200);
            albumInfoIndex.setPlayStatNum(num1);
            albumInfoIndex.setSubscribeStatNum(num2);
            albumInfoIndex.setBuyStatNum(num3);
            albumInfoIndex.setCommentStatNum(num4);

            //5.2 基于统计值计算出专辑得分 为不同统计类型设置不同权重
            BigDecimal bigDecimal1 = new BigDecimal(num4).multiply(new BigDecimal("0.4"));
            BigDecimal bigDecimal2 = new BigDecimal(num3).multiply(new BigDecimal("0.3"));
            BigDecimal bigDecimal3 = new BigDecimal(num2).multiply(new BigDecimal("0.2"));
            BigDecimal bigDecimal4 = new BigDecimal(num1).multiply(new BigDecimal("0.1"));
            BigDecimal hotScore = bigDecimal1.add(bigDecimal2).add(bigDecimal3).add(bigDecimal4);
            albumInfoIndex.setHotScore(hotScore.doubleValue());
        }, threadPoolExecutor);
        //需要等上面四个线程执行完，主线程继续
        CompletableFuture
                .allOf(albumInfoCompletableFuture, categoryViewCompletableFuture, userCompletableFuture, stateCompletableFuture)
                .join();

        //将索引库文档对象存入索引库
        repository.save(albumInfoIndex);

        this.saveSuggestIndex(albumInfoIndex);

        //把专辑id添加到布隆过滤器
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        bloomFilter.add(albumId);

    }

    /**
     * 下架
     */
    public void lower(Long albumId) {
        repository.deleteById(albumId);
    }

    /**
     * 专辑检索
     *
     * @param albumIndexQuery
     * @return
     */
    @Override
    @SneakyThrows
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {

        //构建查询语句
        SearchRequest searchRequest = bulidDSL(albumIndexQuery);
        System.out.println(searchRequest.toString());
        //查询AlbumSearchResponseVo对象
        SearchResponse<AlbumInfoIndex> search = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);
        //封装 AlbumSearchResponseVo对象
        AlbumSearchResponseVo albumSearchResponseVo = this.parseResult(search, albumIndexQuery);
        return albumSearchResponseVo;
    }

    /**
     * 查询指定一级分类下热门排行专辑
     * @param category1Id
     * @return
     */
    @Override
    public List<Map<String,Object>> channel(Long category1Id) {
        try {
            //1.根据1级分类ID远程调用专辑服务获取置顶前7个三级分类集合
            //1.1 远程调用专辑服务获取置顶三级分类集合
            List<BaseCategory3> baseCategory3List = albumFeignClient.findTopBaseCategory3(category1Id).getData();
            Assert.notNull(baseCategory3List, "一级分类{}未包含置顶三级分类", category1Id);
            //1.2 获取所有置顶三级分类ID集合
            List<Long> baseCategory3IdList = baseCategory3List.stream().map(BaseCategory3::getId).collect(Collectors.toList());
            //1.3 将三级分类集合转为Map<三级分类ID，三级分类对象> 方便解析结果封装三级分类对象
            //对BaseCategory3集合处理，转为Map Map中Key:ID，Map中val:三级分类对象BaseCategory3
            Map<Long, BaseCategory3> category3Map = baseCategory3List.stream()
                    .collect(Collectors.toMap(BaseCategory3::getId, baseCategory3 -> baseCategory3));
            //1.4 将置顶三级分类ID转为FieldValue类型
            List<FieldValue> fieldValueList = baseCategory3IdList.stream()
                    .map(baseCategory3Id -> FieldValue.of(baseCategory3Id))
                    .collect(Collectors.toList());
            //2.检索ES获取置顶三级分类（7个）不同置顶三级分类下热度前6个的专辑列表
            //2.1 采用ES检索方法，通过lambda构建请求参数：query,size,aggs
            SearchResponse<AlbumInfoIndex> searchResponse = elasticsearchClient.search(
                    s -> s.index(INDEX_NAME).size(0)
                            .query(q -> q.terms(t -> t.field("category3Id").terms

                                    (tf -> tf.value(fieldValueList))))
                            .aggregations("category3Agg", a -> a.terms(
                                    t -> t.field("category3Id").size(10)
                            ).aggregations("top6Agg", a1 -> a1.topHits(t -> t.size(6).sort(sort -> sort.field(f -> f.field("hotScore").order(SortOrder.Desc)))))),
                    AlbumInfoIndex.class);
            //3.解析ES响应聚合
            System.out.println(searchResponse);
            //3.1 获取三级分类聚合结果对象
            Aggregate category3Agg = searchResponse.aggregations().get("category3Agg");
            //3.2 获取三级分类聚合“桶”集合 由于三级分类ID字段类型为Long调用lterms方法
            Buckets<LongTermsBucket> buckets = category3Agg.lterms().buckets();
            List<LongTermsBucket> bucketList = buckets.array();
            if (CollectionUtil.isNotEmpty(bucketList)) {
                //3.3 遍历“桶”集合，每遍历一个“桶”处理某个置顶三级分类热门专辑
                List<Map<String, Object>> listMap = bucketList.stream().map(bucket -> {
                    Map<String, Object> map = new HashMap<>();
                    //3.3.1 处理热门专辑->分类
                    long category3Id = bucket.key();
                    BaseCategory3 baseCategory3 = category3Map.get(category3Id);
                    map.put("baseCategory3", baseCategory3);
                    //3.3.2 处理热门专辑->专辑列表
                    //3.3.2.1 继续下钻获取子聚合得到当前分类下热门专辑
                    Aggregate top6Agg = bucket.aggregations().get("top6Agg");
                    List<Hit<JsonData>> hits = top6Agg.topHits().hits().hits();
                    if (CollectionUtil.isNotEmpty(hits)) {
                        List<AlbumInfoIndex> hotAlbumList = hits.stream().map(hit -> {
                            //获取专辑JSON对象类型
                            JsonData source = hit.source();
                            return JSON.parseObject(source.toString(), AlbumInfoIndex.class);
                        }).collect(Collectors.toList());
                        map.put("list", hotAlbumList);
                    }
                    return map;
                }).collect(Collectors.toList());
                return listMap;
            }
        } catch (Exception e) {
            log.error("[检索服务]首页热门专辑异常：{}", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    public AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> search, AlbumIndexQuery albumIndexQuery) {
        List<Hit<AlbumInfoIndex>> hits = search.hits().hits();
        AlbumSearchResponseVo albumSearchResponseVo = new AlbumSearchResponseVo();
        List<AlbumInfoIndexVo> list = albumSearchResponseVo.getList();
        for (Hit<AlbumInfoIndex> hit : hits) {
            AlbumInfoIndex source = hit.source();
            AlbumInfoIndexVo albumInfoIndexVo = BeanUtil.copyProperties(source, AlbumInfoIndexVo.class);
            list.add(albumInfoIndexVo);
        }
        long total = search.hits().total().value();
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        albumSearchResponseVo.setTotal(total);
        albumSearchResponseVo.setPageNo(pageNo);
        albumSearchResponseVo.setPageSize(pageSize);
        //总页数
        Long totalPages = total % pageSize == 0 ? total / pageSize : total / pageSize + 1;
        albumSearchResponseVo.setTotalPages(totalPages);
        return albumSearchResponseVo;
    }

    public SearchRequest bulidDSL(AlbumIndexQuery albumIndexQuery) {
        SearchRequest.Builder search = new SearchRequest.Builder();
        search.index(INDEX_NAME);
        BoolQuery.Builder allboolQuery = new BoolQuery.Builder();
        String keyword = albumIndexQuery.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            BoolQuery.Builder keyWordBoolQuery = new BoolQuery.Builder();
            keyWordBoolQuery.should(s -> s.match(m -> m.field("albumTitle").query(keyword)));
            keyWordBoolQuery.should(s -> s.match(m -> m.field("albumIntro").query(keyword)));
            keyWordBoolQuery.should(s -> s.term(t -> t.field("announcerName").value(keyword)));
            allboolQuery.must(keyWordBoolQuery.build()._toQuery());
        }
        if (albumIndexQuery.getCategory1Id() != null && albumIndexQuery.getCategory1Id() > 0) {
            allboolQuery.filter(f -> f.term(t -> t.field("category1Id").value(albumIndexQuery.getCategory1Id())));
        }
        if (albumIndexQuery.getCategory2Id() != null && albumIndexQuery.getCategory2Id() > 0) {
            allboolQuery.filter(f -> f.term(t -> t.field("category2Id").value(albumIndexQuery.getCategory2Id())));
        }
        if (albumIndexQuery.getCategory3Id() != null && albumIndexQuery.getCategory3Id() > 0) {
            allboolQuery.filter(f -> f.term(t -> t.field("category3Id").value(albumIndexQuery.getCategory3Id())));
        }
        List<String> attributeList = albumIndexQuery.getAttributeList();
        if (CollectionUtil.isNotEmpty(attributeList)) {
            for (String attribute : attributeList) {
                String[] split = attribute.split(":");
                if (split != null && split.length == 2) {
                    allboolQuery.filter(
                            f -> f.nested(n -> n.path("attributeValueIndexList")
                                    .query(q -> q.bool(
                                            b -> b.must(m -> m.term(t -> t.field("attributeValueIndexList.attributeId").value(split[0])))
                                                    .must(m -> m.term(t -> t.field("attributeValueIndexList.valueId").value(split[1])))
                                    ))
                            ));
                }
            }
        }

        search.query(allboolQuery.build()._toQuery());
        //排序
        if (StringUtils.isNotBlank(albumIndexQuery.getOrder())) {
            String order = albumIndexQuery.getOrder();
            String[] split = order.split(":");
            if (split != null && split.length == 2) {
                String field = "";
                switch (split[0]) {
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "playStatNum";
                        break;
                    case "3":
                        field = "createTime";
                        break;

                }

                String finalField = field;
                search.sort(s -> s.field(f -> f.field(finalField).order(split[1].equals("asc") ? SortOrder.Asc : SortOrder.Desc)));
            }
        }
        //分页
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        Integer currentPage = (pageNo - 1) * pageSize;
        search.from(currentPage);
        search.size(pageSize);
        //5.设置请求体参数"highlight" 处理高亮，前提：用户录入关键字
        if (StringUtils.isNotBlank(keyword)) {
            search.highlight(h -> h.fields("albumTitle", hf -> hf.preTags("<font style='color:red'>").postTags("</font>")));
        }
        search.source(s -> s.filter(f -> f.excludes("category1Id",
                "category2Id",
                "category3Id",
                "attributeValueIndexList.attributeId",
                "attributeValueIndexList.valueId")));

        return search.build();

    }

    @Override
    public void saveSuggestIndex(AlbumInfoIndex albumInfoIndex) {
        SuggestIndex suggestIndex = new SuggestIndex();
        suggestIndex.setId(albumInfoIndex.getId().toString());
        Pinyin4jEngine pinyin4jEngine = new Pinyin4jEngine();
        String KeywordPinyin = pinyin4jEngine.getPinyin(albumInfoIndex.getAlbumTitle(), "");
        String KeywordSequence = pinyin4jEngine.getFirstLetter(albumInfoIndex.getAlbumTitle(), "");

        suggestIndex.setTitle(albumInfoIndex.getAlbumTitle());
        suggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAlbumTitle()}));
        suggestIndex.setKeywordPinyin(new Completion(new String[]{KeywordPinyin}));
        suggestIndex.setKeywordSequence(new Completion(new String[]{KeywordSequence}));

        suggestIndexRepository.save(suggestIndex);
    }

    /**
     * 关键字补充
     * @param keyword
     * @return
     */
    @Override
    @SneakyThrows
    public List<String> completeSuggest(String keyword) {
        SearchResponse<SuggestIndex> suggestSearch = elasticsearchClient.search(s -> s.index("suggestinfo")
                .suggest(sg -> sg.suggesters("mySuggestKeyword", sgs -> sgs.prefix(keyword).completion(c -> c.field("keyword").skipDuplicates(true).size(10)))
                        .suggesters("mykeywordPinyin", sgs -> sgs.prefix(keyword).completion(c -> c.field("keywordPinyin").skipDuplicates(true).size(10)))
                        .suggesters("mykeywordSequence", sgs -> sgs.prefix(keyword).completion(c -> c.field("keywordSequence").skipDuplicates(true).size(10)))
                ), SuggestIndex.class);
        System.out.println(suggestSearch);
        Set<String> hashSet=new HashSet<>();
        hashSet.addAll(this.parseSuggestResult("mySuggestKeyword", suggestSearch));
        hashSet.addAll(this.parseSuggestResult("mykeywordPinyin", suggestSearch));
        hashSet.addAll(this.parseSuggestResult("mykeywordSequence", suggestSearch));

        //判断结果够不够十条
        if (hashSet.size()>=10){
            return new ArrayList<>(hashSet).subList(0,10);
        }

        SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(s -> s.index(INDEX_NAME).query(q -> q.match(m -> m.field("albumTitle").query(keyword))), AlbumInfoIndex.class);
        //解析检索结果，将结果放入HashSet
        List<Hit<AlbumInfoIndex>> hits = response.hits().hits();
        if (CollectionUtil.isNotEmpty(hits)) {
            for (Hit<AlbumInfoIndex> hit : hits) {
                AlbumInfoIndex albumInfoIndex = hit.source();
                hashSet.add(albumInfoIndex.getAlbumTitle());
                if (hashSet.size() >= 10) {
                    break;
                }
            }
        }
        //4.最多返回10个自动补全提示词
        return new ArrayList<>(hashSet);
    }

    /**
     * 专辑详情
     * @param albumId
     * @return
     */
    @Override
    public Map<String, Object> albumInfo(Long albumId) {
        //使用布隆过滤器判断是否存在
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        if (!bloomFilter.contains(albumId)) {
            throw new GuiguException(404, "访问专辑不存在");
        }

        //这里涉及到多线程共享资源问题需要使用线程安全的map
        Map<String, Object> map = new ConcurrentHashMap<>();
        //1.获取专辑信息
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            map.put("albumInfo", albumInfo);
            return albumInfo;
        }, threadPoolExecutor);
        //2.获取用户信息
        CompletableFuture<Void> userCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync((albumInfo) -> {
            UserInfo userInfo = userFeignClient.getUserInfoById(albumInfo.getUserId()).getData();
            UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
            map.put("announcer", userInfoVo);
        }, threadPoolExecutor);
        //3.获取分类信息
        CompletableFuture<Void> categoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync((albumInfo) -> {
            BaseCategoryView baseCategoryView = albumFeignClient.getbaseCategoryViewById(albumInfo.getCategory3Id()).getData();
            map.put("baseCategoryView", baseCategoryView);
        }, threadPoolExecutor);
        //4.获取统计信息
        CompletableFuture<Void> albumStatVoCompletableFuture = CompletableFuture.runAsync(() -> {
            AlbumStatVo albumStatVo = albumFeignClient.getAlbumStatVo(albumId).getData();
            map.put("albumStatVo", albumStatVo);
        }, threadPoolExecutor);
        CompletableFuture.allOf(albumInfoCompletableFuture,userCompletableFuture,albumStatVoCompletableFuture,categoryViewCompletableFuture)
                .join();
        return map;
    }

    /**
     * 获取不同分类下不同排序方式榜单专辑列表
     */
    @Override
    public void updateLatelyAlbumRanking() {
        try {
            //1.远程调用专辑服务获取所有1级分类列表
            List<BaseCategory1> category1List = albumFeignClient.getAllCategory1().getData();
            Assert.notNull(category1List, "一级分类为空");

            //2.循环遍历1级分类列表，获取该分类下5种不同排序方式榜单专辑
            for (BaseCategory1 baseCategory1 : category1List) {
                Long category1Id = baseCategory1.getId();
                //3.在处理当前1级分类中，再次循环5种不同排序方式得到具体榜单数据
                //3.1 声明排序方式数组
                String[] rankingDimensionArray =
                        new String[]{"hotScore", "playStatNum", "subscribeStatNum", "buyStatNum", "commentStatNum"};
                for (String rankingDimension : rankingDimensionArray) {
                    //3.2 调用ES检索接口获取榜单数据
                    SearchResponse<AlbumInfoIndex> searchResponse = elasticsearchClient.search(
                            s -> s.index(INDEX_NAME)
                                    .query(q -> q.term(t -> t.field("category1Id").value(category1Id)))
                                    .sort(sort -> sort.field(f -> f.field(rankingDimension).order(SortOrder.Desc)))
                                    .size(10)
                            ,
                            AlbumInfoIndex.class
                    );
                    //3.3 获取当前分类下某个排序方式榜单专辑列表
                    List<Hit<AlbumInfoIndex>> hits = searchResponse.hits().hits();
                    if (CollectionUtil.isNotEmpty(hits)) {
                        List<AlbumInfoIndex> list = hits.stream().map(hit -> hit.source()).collect(Collectors.toList());
                        //4.将榜单专辑列表存入Redis-Hash中
                        //4.1 声明Redis排行榜Hash接口 Key 形式：前缀+1级分类ID     field：排序方式  val:榜单列表
                        String key = RedisConstant.RANKING_KEY_PREFIX + category1Id;
                        //4.2 将当前分类榜单数据放入Redis中
                        redisTemplate.opsForHash().put(key, rankingDimension, list);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[搜索服务]更新排行榜异常：{}", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<AlbumInfoIndex> findRankingList(String category1Id, String dimension) {
        String key = RedisConstant.RANKING_KEY_PREFIX + category1Id;
        List<AlbumInfoIndex> albumInfoIndexList = (List<AlbumInfoIndex>) redisTemplate.opsForHash().get(key, dimension);
        return albumInfoIndexList;
    }

    private List<String> parseSuggestResult(String suggestKeyword, SearchResponse<SuggestIndex> suggestSearch) {
        List<String> titleList=new ArrayList<>();
        List<Suggestion<SuggestIndex>> suggestions = suggestSearch.suggest().get(suggestKeyword);
        //suggestions这个结果不会为null
        for (Suggestion<SuggestIndex> suggestion : suggestions) {
            List<CompletionSuggestOption<SuggestIndex>> options = suggestion.completion().options();
            //options这个结果可能为null
            if (CollectionUtil.isNotEmpty(options)){
                for (CompletionSuggestOption<SuggestIndex> option : options) {
                    String title = option.source().getTitle();
                    titleList.add(title);
                }
            }
        }
        return titleList;
    }

}
