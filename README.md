## &nbsp;
## FlashTsDB时序数据库介绍


### 1、What is FlashTsDB
#### 1.1 背景

随着互联网、物联网的不断发展，对于海量具有时序特征数据的存储需求日益增多，TSDB（时序数据库）开始受到更多的关注。传统的商业时序数据库（例如OSI-PI）价格昂贵；Influxdb作为目前开源排名最高的时序数据库，集群版已经闭源商业化，开源版仅支持单机模式；OpenTsDB集群方案成熟（Hbase），但实测压缩比不高，性能也是差强人意。
在这样的大环境下，当前市面上无论是开源时序库还是商业时序库都有各自的问题，很难找到一款免费、稳定、高压缩比、高性能、可扩展的时序数据库软件。

![image](https://github.com/amon1991/flashdb/blob/master/src/main/resources/introimages/1.png)


#### 1.2 FlashTsDB

FlashTsDB开发语言主要为Java，其基于SDT压缩算法、Hbase及Redis实现，具有可配置压缩比，高性能，可扩展等特性，FlashTsDB设计之初主要面对纯粹的设备类（例如传感器）数据，主要追求高压缩率、高性能、可扩展和支持线性插值等特性，根据性能测试结果，在同等硬件环境下，其存储和查询性能均优于influxdb。

FlashTsDB同时支持单机和集群模式。单机模式下，可以直接通过docker镜像部署，整个部署过程非常简单，性能也足以满足一般场景下的需求。


#### 1.3 时序数据库概念
FlashTsDB定位于时序数据库。

时序数据是基于时间的一系列的数据。在有时间的坐标中将这些数据点连成线，往过去看可以做成多纬度报表，揭示其趋势性、规律性、异常性；往未来看可以做大数据分析，机器学习，实现预测和预警。
时序数据库就是存放时序数据的数据库，并且需要支持时序数据的快速写入、持久化、多纬度的聚合查询等基本功能。


### 2、Why FlashTsDB

FlashTsDB着眼于解决时序库的最核心问题，目前提供了如下特性：

* **自定义压缩率、支持插值查询**
	* FlashTsDB支持SDT压缩算法，可以自定义旋转门大小，控制数据实际压缩率；
	* 支持线性插值算法，可插值查询任意时刻的测点数值

* **分布式存储**
	* FlashTsDB底层使用hbase进行数据持久化，支持分布式存储
	* 测点的实时数据存储在Redis中，查询实时数据性能高
	
* **web端查询**
	* FlashTsDB支持在web端进行测点删选、历史数据查询、实时数据查询
	* 历史数据查询的结果通过趋势图展示
	
* **提供开放平台API**
	* FlashTsDB支持Restful API新增测点、插入测点数据、查询测点数据
	
* **高性能**
	* 经测试，在千兆网络情况下，单进程模式下的FlashTsDB写入即可打满40%的网络带宽，查询可打满90%的网络带宽
	* 在高并发场景下，FlashTsDB表现稳定，除平均响应时延增加外，无失败请求

* **部署简单**
	* FlashTsDB仅依赖hbase集群和redis数据库
	* 可基于docker方式直接部署，部署方便


### 3、FlashTsDB at a glance
#### 3.1 界面概览

![image](https://github.com/amon1991/flashdb/blob/master/src/main/resources/introimages/2.png)

上图是FlashTsDB的主界面
* 在页面左上方可查询当前数据库中的测点列表（支持模糊查询）
* 在页面右上方可新建测点
* 对于每个测点，可查询详情

![image](https://github.com/amon1991/flashdb/blob/master/src/main/resources/introimages/3.png)

上图为新建测点界面，其中可通过设置accuracy（精确度）设置压缩时旋转门的大小


#### 3.2 查询测点实时/历史数据

![image](https://github.com/amon1991/flashdb/blob/master/src/main/resources/introimages/4.png)

* 在上图中，可查询测点的实时数据和历史数据
* 历史数据查询支持查询实际数据和插值数据

#### 3.3 Restful API

![image](https://github.com/amon1991/flashdb/blob/master/src/main/resources/introimages/5.png)

用户可通过swagger界面查询和测试FlashTsDB支持的客户端API，具体支持的API功能如下所示：

##### 3.3.1 创建测点元数据

 **URL**
 - /apis/flashtsdb/tags

 **请求类型**
 - POST

 **请求示例**
```java
[
  {
    "accuracyE": 5,
    "tagCode": "testSensorCode",
    "tagDescription": "testSensor Des",
    "tagName": "testSensorName",
    "tagUnit": "ka"
  }
]
```
 **请求参数说明**
|参数名|类型|说明|
|:-----  |:-----|-----                           |
| accuracyE|数值   |测点精确度（decimals,greater than 0）|
| tagCode| 字符串  |测点编码 （limit 200 characters）|
| tagDescription| 字符串  |测点描述 （limit 200 characters）|
| tagName| 字符串  |测点名称（limit 200 characters） |
| tagUnit|  字符串 |测点单位 （limit 50 characters）|

 **返回示例**
```java
{
  "success": true,
  "msg": "create tags successfully"
}
```
 **返回参数说明** 

|参数名|类型|说明|
|:-----  |:-----|-----          |
|success |boolean   |true or false |
|msg |string   |提示信息 |

##### 3.3.2 查询全量测点列表

 **URL**
 - /apis/flashtsdb/tags

 **请求类型**
 - GET

 **返回示例**
```java

{
  "success": true,
  "msg": "get tags successfully",
  "data": [
    {
      "tagCode": "tagcode8da73d07-48c0-4347-a99c-7442449260ff",
      "tagName": "tagName",
      "tagDescription": "tagDescription",
      "tagUnit": "ka",
      "accuracyE": 20,
      "createtime": 1607928817528
    },
    {
      "tagCode": "tagcodeddac5b1a-26b2-477d-99c3-25e52d85491c",
      "tagName": "tagName",
      "tagDescription": "tagDescription",
      "tagUnit": "ka",
      "accuracyE": 20,
      "createtime": 1607928817529
    }]
 }
```
 **返回参数说明** 

|参数名|类型|说明|
|:-----  |:-----|-----          |
|tagCode |字符串   |测点编码 |
|tagName |字符串   |测点名称 |
|tagDescription |字符串   |测点描述 |
|tagUnit |字符串   |测点单位 |
|accuracyE |数值   |精确度 |
|createtime |数值   |时间戳，创建时间 |

##### 3.3.3 模糊查询测点列表

 **URL**
 - /apis/flashtsdb/tags/{regex}

 **请求类型**
 - GET
 **请求示例**
```java
/apis/flashtsdb/tags/test
```
 **返回示例**
```java
{
  "success": true,
  "msg": "get tags successfully",
  "data": [
    {
      "tagCode": "testSensorCode",
      "tagName": "testSensorName",
      "tagDescription": "testSensor Des",
      "tagUnit": "ka",
      "accuracyE": 5,
      "createtime": 1608704579848
    }
  ]}
```
 **返回参数说明** 

|参数名|类型|说明|
|:-----  |:-----|-----          |
|tagCode |字符串   |测点编码 |
|tagName |字符串   |测点名称 |
|tagDescription |字符串   |测点描述 |
|tagUnit |字符串   |测点单位 |
|accuracyE |数值   |精确度 |
|createtime |数值   |时间戳，创建时间 |##### 3.3.2 查询所有测点列表

##### 3.3.4 批量写入测点历史值

 **URL**
 - /apis/flashtsdb/points

 **请求类型**
 - POST

 **请求示例**
```java
{
  "savingMode": 0,
  "tagPointLists": [
    {
      "pointList": [
        {
          "x": 1608707040000,
          "y": 100
        },
        {
          "x": 1608707340000,
          "y": 200
        }
      ],
      "tag": "testSensorCode"
    }
  ]
}
```
 **请求参数说明**
|参数名|类型|说明|
|:-----  |:-----|-----                           |
| savingMode|数值   |写入模式：0：覆盖写；1：追加写|
| tagPointLists| 对象数组  |写入测点集合|

--tagPointLists
|参数名|类型|说明|
|:-----  |:-----|-----                           |
| pointList|  对象数组 |x：时间戳；y：测点值|
| tag| 字符串  |测点编码|

 **返回示例**
```java

{
  "success": true,
  "msg": "save data successfully"}
```
 **返回参数说明** 

|参数名|类型|说明|
|:-----  |:-----|-----          |
|success |boolean   |true or false |
|msg |string   |提示信息 |

##### 3.3.5 查询测点实时值

 **URL**
 - /apis/flashtsdb/points/realtime

 **请求类型**
 - POST

 **请求示例**
```java
[
  "testSensorCode"
]
```
 **请求参数说明**
|参数名|类型|说明|
|:-----  |:-----|-----                           |
| --|字符串数组   |测点编码集合|

 **返回示例**
```java
{
  "data": {
    "testSensorCode": {
      "x": 1608707340000,
      "y": 200
    }
  },
  "success": true,
  "msg": "get points data successfully"}
```
 **返回参数说明** 

|参数名|类型|说明|
|:-----  |:-----|-----          |
|success |boolean   |true or false |
|msg |string   |提示信息 |
|data |map   |key：测点编码；value：测点实际值 |

##### 3.3.6 查询测点历史值

 **URL**
 - /apis/flashtsdb/points/historcal

 **请求类型**
 - POST

 **请求示例**
```java
{
  "bgTime": 1608707040000,
  "endTime": 1608707440000,
  "limit": -1,
  "searchInterval": 30,
  "searchMode": 1,
  "tagList": [
    "testSensorCode"
  ]
}
```
 **请求参数说明**
|参数名|类型|说明|
|:-----  |:-----|-----                           |
| bgTime   |long|查询开始时间|
| endTime   |long|查询结束时间|
| limit   |数值|返回记录数限制，-1代表无限制|
| searchInterval   |数值|插值间隔|
| searchMode   |数值|0：实际值；1：插值|
| tagList   |对象数组|测点编码集合|

 **返回示例**
```java
{
  "data": [
    {
      "tag": "testSensorCode",
      "pointList": [
        {
          "x": 1608707040000,
          "y": 100
        },
        {
          "x": 1608707070000,
          "y": 110
        },
        {
          "x": 1608707100000,
          "y": 120
        },
        {
          "x": 1608707130000,
          "y": 130
        },
        {
          "x": 1608707160000,
          "y": 140
        },
        {
          "x": 1608707190000,
          "y": 150
        },
        {
          "x": 1608707220000,
          "y": 160
        },
        {
          "x": 1608707250000,
          "y": 170
        },
        {
          "x": 1608707280000,
          "y": 180
        },
        {
          "x": 1608707310000,
          "y": 190
        },
        {
          "x": 1608707340000,
          "y": 200
        }
      ]
    }
  ],
  "success": true,
  "msg": "search points data successfully"
  }
```
 **返回参数说明** 

|参数名|类型|说明|
|:-----  |:-----|-----          |
|success |boolean   |true or false |
|msg |string   |提示信息 |
|data |对象数组   |测点历史数据 |
#### 3.4 总体设计

![image](https://github.com/amon1991/flashdb/blob/master/src/main/resources/introimages/6.png)

FlashTsDB使用Redis存储测点元数据和实时数据，使用Hbase存储测点历史数据，数据存储压缩与解压查询均在FlashDB核心进程中实现，核心功能使用Java开发，其主要实现技术栈如下：

* Jdk 1.8
* Spring Boot 2.x
* Hbase 1.x
* Redis 6.x
* Layui
* Echarts

#### 4、Deployment

##### 4.1 单机部署
FlashTsDB镜像地址：https://hub.docker.com/r/amon1991/flashtsdb/tags

**pull redis、hbase、flashtsdb镜像**
```java
docker pull redis

docker pull harisekhon/hbase:1.3

docker pull amon1991/flashtsdb:1.0.1
```
**启动docker镜像**
```java
docker run -d -v /redis-data:/data -p 6379:6379 --name myredis redis

docker run -d -v /hbase-data:/hbase-data -h myhbase -p 2181:2181 -p 16000:16000 -p 16010:16010 -p 16020:16020 -p 16030:16030 --name myhbase harisekhon/hbase:1.3

docker run -e BOOT_OPTIONS="-Xms4g -Xmx4g -Xmn3g" -d -p 8066:8066 --link myredis --link myhbase --name flashtsdb amon1991/flashtsdb:1.0.1
```
**验证部署是否成功**
hbase网页地址：http://部署机ip:16010/master-status
flash网页地址：http://部署机ip:8066/portal/index

##### 4.1 集群部署
修改源码中的配置文件：
```java
hbase:
  config:
    hbase.zookeeper.quorum: zookeeper1:2181,zookeeper2:2181,zookeeper3:2181
    hbase.compression.algorithm: SNAPPY
```
配置zookeeper地址指向hbase集群
编译打包，启动FlashTsDB进程：
```java
java -jar flashtsdb-1.0.1-RELEASE.jar
```
#### 5、Contribute to FlashTsDB

FlashTsDB从开发之初就是以开源模式开发的，所以也非常欢迎有兴趣、有余力的朋友一起加入进来。

服务端开发使用的是Java，基于Spring Boot 2.x框架。客户端目前提供了Restful API实现，正在规划引入RPC类型的实现方式。

欢迎大家发起Pull Request！
