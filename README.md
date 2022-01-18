# Eureka Subscribe Adapter
Eureka 订阅模式扩展模块，可实时感知Eureka注册中心里服务列表的变化。Eureka Server与Eureka Client的扩展。
其中Eureka Client一侧支持Netflix Ribbon、Spring Cloud Loadbalancer的服务列表实时感知。

# 实现原理
利用Servlet 3的异步机制，对订阅请求进行挂起并不立即写回响应。同时Eureka服务中注册一个监听器，
监听服务上线、下线的事件，只有触发对应事件时才会写回响应。  
服务端的超时时间默认为30s, 也就是说客户端的等待超时时间需要大于等于30秒。
客户端如果要持续监听事件则只需要轮询订阅接口即可。

# 暴露接口
````http request
GET /eureka/subscribe/{appName}
````
Header 参数:
> read-timeout: 默认值30，单位为秒。该值必须大于1。说明：后端处理时会用该值减去1作为等待超时时间，防止踩点响应导致客户端出现读超时异常。
> 当 read-timeout<=1 时，会使用默认值30处理请求.  
> app-hash: 非必填且默认为空。该值用于比对服务器中的服务列表Hash值，如果客户端传递的Hash值与Eureka服务端的一致，则说明服务列表没有发生变化，
> 此时请求不会立即响应。当客户端传递的app-hash与服务端的hash值不一致时，则表示客户端与服务端的服务列表不一致，需要立即返回当前服务端的最新服务列表。


# 版本要求
> JDK >= 1.8  
> Spring Boot >= 2.1.X  
> Spring Cloud >= Greenwich

# 使用方法
### Eureka Server 端
### 一.添加依赖方式
* 1.先安装代码到本地仓库(已安装则跳过)
````shell script
mvn clean install
````
* 2.在Eureka Server模块中引入以下依赖
```` java
<dependency>
    <groupId>cn.icodening.eureka</groupId>
    <artifactId>eureka-subscribe-server</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
````
* 3.在bootstrap.yml中加入eureka.server.subscribe.enabled=true的配置项
```` yaml
eureka:
  server:
    subscribe:
      enabled: true #不配置时默认为值为true。当不需要启动该模块时可以改为false
````
* 4.启动Eureka Server


---
### Client 端
### 支持Ribbon、Spring Cloud Loadbalancer
* 1.先安装代码到本地仓库(已安装则跳过)
````shell script
mvn clean install
````
* 2.在应用的POM中引入以下依赖  
````shell script
<!--ribbon-->
<dependency>
    <groupId>cn.icodening.eureka</groupId>
    <artifactId>spring-cloud-starter-eureka-subscribe-ribbon</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
````
````shell script
<!--spring cloud loadbalancer-->
<dependency>
    <groupId>cn.icodening.eureka</groupId>
    <artifactId>spring-cloud-starter-eureka-subscribe-loadbalancer</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
````
* 3.在bootstrap.properties/yml中加入以下配置.  
>开关配置项命名规则为 [组件名.eureka.subscribe.enabled]
```` properties
# ribbon订阅开关
ribbon.eureka.subscribe.enabled=true
````
```` properties
# spring cloud loadbalancer订阅开关
spring.cloud.loadbalancer.eureka.subscribe.enabled=true
````
* 4.启动应用  
---