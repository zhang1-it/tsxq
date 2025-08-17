# 项目背景
随着智能手机和高速互联网的普及，人们开始寻求更便捷的方式来获取信息和娱乐。有声书的出现使得人们可以在旅途中、跑步时、做家务时等各种场景下，以更加灵活的方式享受阅读。

在过去，有声书主要是由**专业的演员朗读**，制作成录音带或CD。但随着数字化媒体的发展，听书软件应运而生，为用户提供了更多选择，包括自助出版的有声书和多样化的内容。
# 项目运行图
### 主界面
<img width="404" height="705" alt="image" src="https://github.com/user-attachments/assets/2cd58e77-956d-4266-86cd-368a12b2e4d1" />

### 排行榜
<img width="393" height="678" alt="image" src="https://github.com/user-attachments/assets/d3bf2f94-5133-4744-a2d4-6e5a18c195dd" />

### 个人主页
<img width="385" height="676" alt="image" src="https://github.com/user-attachments/assets/24441a88-e7fe-4112-a9bb-9ff6ca95d1e1" />

### 搜索
<img width="385" height="668" alt="image" src="https://github.com/user-attachments/assets/75ca11af-053f-4f73-8170-94b49fc1ed57" />
<img width="387" height="682" alt="image" src="https://github.com/user-attachments/assets/51c465cc-f518-402f-8e05-392bb44ed621" />

### 全部分类
<img width="382" height="687" alt="image" src="https://github.com/user-attachments/assets/cab6c8f5-4e7b-4e9c-9740-e16364ad828b" />

### 分类下全部专辑
<img width="393" height="680" alt="image" src="https://github.com/user-attachments/assets/da5700af-e4dc-4391-a8e0-bfe1e4efd007" />

### 专辑详情
<img width="397" height="685" alt="image" src="https://github.com/user-attachments/assets/255694da-9c54-4689-8ea8-184411d37796" />

### 声音详情
<img width="390" height="682" alt="image" src="https://github.com/user-attachments/assets/52f76e2e-d001-4d94-a3cf-d2fb06c024ba" />

### vip购买
<img width="392" height="705" alt="image" src="https://github.com/user-attachments/assets/55454ce3-53ba-4c42-bc0b-005ba597fde0" />
<img width="400" height="703" alt="image" src="https://github.com/user-attachments/assets/e9d3804b-25c9-4e51-a7cd-80def71b6472" />
<img width="399" height="699" alt="image" src="https://github.com/user-attachments/assets/d8106fa9-5959-4a15-b84e-d1bfefb32367" />

### 声音购买
<img width="400" height="714" alt="image" src="https://github.com/user-attachments/assets/58a3f10c-e8ce-406a-8893-95f304176a1f" />
<img width="391" height="696" alt="image" src="https://github.com/user-attachments/assets/affbf537-b732-4069-8ad7-408fc7c66359" />

### 我的钱包
<img width="390" height="691" alt="image" src="https://github.com/user-attachments/assets/d7e64cda-5b55-4fe4-b9b5-656cc8222d80" />
<img width="400" height="692" alt="image" src="https://github.com/user-attachments/assets/1df65fb9-a277-4c66-8605-702c0fcb76e8" />
<img width="387" height="680" alt="image" src="https://github.com/user-attachments/assets/30ce7f62-db1d-4188-b463-ad7ca01dd889" />
<img width="388" height="697" alt="image" src="https://github.com/user-attachments/assets/b5bc18f9-8ecd-479e-b05c-5c1086988f73" />

### 订单中心
<img width="375" height="658" alt="image" src="https://github.com/user-attachments/assets/9702b3e9-24c1-469f-a1e3-f88ae5c66311" />

### 我的专辑
<img width="386" height="693" alt="image" src="https://github.com/user-attachments/assets/c5b234f8-39b5-44c7-87cf-55bcb74eb523" />
<img width="395" height="708" alt="image" src="https://github.com/user-attachments/assets/cea63a79-df4e-4bc1-982c-32d4daa7a2eb" />

### 我的声音
<img width="398" height="707" alt="image" src="https://github.com/user-attachments/assets/c6d69319-8d96-4bc4-ad9b-eaaf4b5bd167" />
<img width="391" height="679" alt="image" src="https://github.com/user-attachments/assets/69ad72dc-b8fb-49d1-a2bc-b86f6b91254b" />

# 文件结构
**mp-weixin** 前端代码

**tingshu-parent** 后端代码

## 项目结构
```
tingshu-parent/
├── common/ # 公共模块
│ ├── common-util/ # 通用工具类
| ├── kafka-util/ # 消息队列工具类
│ └── service-util/ # 服务通用组件
├── model/ # 数据模型
├── service-gateway/ # 网关
├── service/ # 业务服务模块
│ ├── service-search/ # 搜索服务
│ └── ... # 其他服务模块
├── service-client/ #远程服务客户端
│ ├── service-account-client/ #账户远程调用客户端
│ └── ... # 其他客户端模块
└── pom.xml # 父项目配置
```

# 技术栈
- **SpringBoot**：简化Spring应用的初始搭建以及开发过程
- **SpringCloud**：基于Spring Boot实现的云原生应用开发工具，SpringCloud使用的技术：（Spring Cloud Gateway、Spring Cloud Task和Spring Cloud Feign、Spring Cloud Nacos、Sentinel等）
- MyBatis-Plus：持久层框架（简化持久层开发）基于Mybatis
- Redis：内存做缓存
- **Redisson**：基于redis的Java驻内存数据网格 - 框架；操作redis的框架
- **MongoDB**: 分布式文档型数据库
- Kafka：消息中间件；大型分布式项目是标配；分布式事务最终一致性
- **ElasticSearch**+Kibana+Logstash/Filebeat 全文检索服务器+可视化数据监控：检索
- ThreadPoolExecutor+**CompletableFuture**：线程池来实现异步操作，提高效率 
- **Xxl-Job**: 分布式定时任务调用中心
- Swagger/Knife4J/YAPI：Api接口文档工具
- MinIO（私有化对象存储集群）：分布式文件存储系统 类似于OSS（公有）
- 在线支付平台：**微信支付**
- MySQL：关系型数据库
- Hutool：Java工具类库
- Lombok: 实体类的中get/set 生成的jar包
- **Natapp**：内网穿透工具
- Docker：容器化技术;  生产环境（运维人员）；快速搭建环境
- Git：代码管理工具；git使用，拉代码、提交、推送、合并、冲突解决
- **Canal**：阿里开源增量订阅组件，数据增量同步
- **Seata**：阿里开源分布式事务解决方案

前端技术栈

- UniApp
- Vue3全家桶
- TypeScript
- GraceUI
- UniUI
- uniapp-axios-adapter
# 搭建项目
## 环境初始化
- 本项目环境搭建使用的CentOs7系统，请先准备一台可以创建CentOs7虚拟机的电脑。
- 点击[百度网盘](https://pan.baidu.com/s/1Qq3oIe4Q7fYl_q2wKKZsXw?pwd=arn3)进行下载，使用VmWare打开
- 需要修改虚拟机Net模式的网络配置，本项目虚拟机的ip地址为192.168.254.156  账号：root  密码：root
- <img width="1175" height="810" alt="image" src="https://github.com/user-attachments/assets/f5913c58-45f4-4aae-80e6-21cf23e5abc8" />

## 前端初始化
- 拉取mp-weixin到本地
- 下载微信开发者工具
- 注册账号
- 使用微信开发者工具打开项目并启动
## 后端初始化
- 下载idea
- 下载maven
- 下载jdk17
- 拉取tingshu-parent到本地
- 使用idea打开，等待maven下载依赖
- 启动
  # 可能遇到的问题
  ## 声音上传功能中，音频上传失败
  **解决办法：** 本项目音频上传功能使用的腾讯云点播进行完成的，所以需要配置腾讯云点播所需要的环境，点击[云点播 快速入门-文档中心-腾讯云 (tencent.com)](https://cloud.tencent.com/document/product/266/8757)进行注册，在nacos中修改**service-album-dev.yaml**文件，添加如下内容
  ```
  vod:
  appId:  #需要修改为自己的
  secretId:  #需要修改为自己的
  secretKey:  #需要修改为自己的
  region:  #需要修改为自己的
  procedure: SimpleAesEncryptPreset #任务流
  #tempPath: /root/tingshu/tempPath
  tempPath: D:\code\workspace2023\tingshu\temp
  playKey: wrTwwu8U3DRSRDgC8l7q  #播放加密key
  ```
  ## 微信登录功能无法使用
  **解决方法：**[小程序登录 | 微信开放文档 (qq.com)](https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/login.html)。在nacos中修改**service-user-dev.yaml**文件，修改如下内容
  ```
  wx:
  miniapp:
    appid:   # 小程序微信公众平台appId 改成自己的申请测试号应用id
    secret:  # 小程序微信公众平台api秘钥 改成自己的申请测试号秘钥
    msgDataFormat: JSON
  ```
## 其他问题请在邮箱中留言：2660393395@qq.com
