package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseAttributeService;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.album.service.BaseCategoryViewService;
import com.atguigu.tingshu.common.cache.GuiguCache;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Tag(name = "分类管理")
@RestController
@RequestMapping(value="/api/album")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {

	@Autowired
	private BaseCategoryService baseCategoryService;
	@Autowired
	private BaseCategoryViewService baseCategoryViewService;
	@Autowired
	private BaseAttributeService baseAttributeService;

	/**
	 * 查询所有一级分类列表
	 * @return
	 */
	@Operation(summary = "查询所有一级分类列表")
	@GetMapping("/category/findAllCategory1")
	public Result<List<BaseCategory1>> getAllCategory1() {
		return Result.ok(baseCategoryService.list());
	}
	/**
	 * 根据1级分类对象查询包含二级分类（包含三级分类）
	 * @param category1Id
	 * @return
	 */
	@GetMapping("/category/getBaseCategoryList/{category1Id}")
	public Result<JSONObject> getBaseCategoryListByCategory1Id(@PathVariable Long category1Id){
		JSONObject jsonObject = baseCategoryService.getBaseCategoryListByCategory1Id(category1Id);
		return Result.ok(jsonObject);
	}

	/**
	 * 根据一级分类Id查询三级分类列表
	 * /api/album/category/findTopBaseCategory3/{category1Id}
	 */
	@GetMapping("/category/findTopBaseCategory3/{category1Id}")
	public Result<List<BaseCategory3>> findTopBaseCategory3(@PathVariable Long category1Id){
		List<BaseCategory3> list = baseCategoryService.findTopBaseCategory3(category1Id);
		return Result.ok(list);
	}

	/**
	 * 获取所有分类
	 * @return
	 */
	@GetMapping("category/getBaseCategoryList")
	public Result<List<JSONObject>> getBaseCategoryList(){
		List<JSONObject> jsonObjectList = baseCategoryViewService.getBaseCategoryList();
		return Result.ok(jsonObjectList);
	}

	/**
	 * 根据一级分类id获取一级分类的标签信息
	 */
	@GetMapping("/category/findAttribute/{category1Id}")
	public Result<List<BaseAttribute>> findAttribute(@PathVariable Long category1Id){
		List<BaseAttribute> list = baseAttributeService.findAttribute(category1Id);
		return Result.ok(list);
	}

	/**
	 * 根据id查询分类信息
	 * @param id
	 * @return
	 */
	@GetMapping("/category/getbaseCategoryViewById/{id}")
	@GuiguCache(prefix = "baseCategoryViewById:")
	public Result<BaseCategoryView> getbaseCategoryViewById(@PathVariable Long id){
		BaseCategoryView baseCategoryView = baseCategoryViewService.getById(id);
		return Result.ok(baseCategoryView);
	}

}

