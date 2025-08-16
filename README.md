# 项目背景
随着智能手机和高速互联网的普及，人们开始寻求更便捷的方式来获取信息和娱乐。有声书的出现使得人们可以在旅途中、跑步时、做家务时等各种场景下，以更加灵活的方式享受阅读。

在过去，有声书主要是由**专业的演员朗读**，制作成录音带或CD。但随着数字化媒体的发展，听书软件应运而生，为用户提供了更多选择，包括自助出版的有声书和多样化的内容。
# 文件结构
**mp-weixin** 前端代码

**tingshu-parent** 后端代码
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
- 需要修改虚拟机网络配置，本项目虚拟机的ip地址为192.168.254.156
- <img width="1175" height="810" alt="image" src="https://github.com/user-attachments/assets/f5913c58-45f4-4aae-80e6-21cf23e5abc8" />

## 前端初始化
- 拉取mp-weixin到本地
- 下载微信开发者工具
- 注册账号
- 使用微信开发者工具打开项目并启动
## 后端初始化
- 下载idea
- 下载maven
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
