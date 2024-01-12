# Build Project Environment

## Project front-end: wechat mini program
- my-qingqi-weixin

## Project back end: docker container
- docker installs mongoDB

~~~shell
#Pull image
docker pull mongo:4.0.3

#Create a container
docker create --name mongodb-qingqi-server -p 27018:27017 -v mongodb-qingqi-server-data:/data/db mongo:4.0.3 --auth

#Starter container
docker start mongodb-qingqi-server

#Enter the container
docker exec -it mongodb-qingqi-server /bin/bash

#Access the admin database
mongo
use admin

#Add an administrator who has permission to manage users and roles
db.createUser({ user: 'root', pwd: 'root', roles: [ { role: "root", db: "admin" } ] })
#Authentication is performed after the exit

#perform the authentication
mongo -u "root" -p "root" --authenticationDatabase "admin"

#Add a common user as admin
use admin
db.createUser({ user: 'qingqi', pwd: 'oudqBFGmGY8pU6WS', roles: [ { role: "readWrite", db: "qingqi" } ] });

#Test with tanhua user login
mongo -u "qingqi" -p "oudqBFGmGY8pU6WS" --authenticationDatabase "admin"

#You can log in to the console to perform operations
~~~

- docker installs redis

~~~shell
docker create --name redis-server -p 6399:6379 --restart=always -v redis-server-data:/data redis:5.0.2 --appendonly yes

docker start redis-server

#Enter the container for testing
docker exec -it redis-server /bin/bash

#test
root@0bd11c170b43:/data# redis-cli 
127.0.0.1:6379> set abc 123
OK
127.0.0.1:6379> get abc
"123"
127.0.0.1:6379> del abc
~~~

## Small program authorization login
- The mini program can easily obtain the user identity provided by wechat through the login ability provided by wechat official, and quickly establish the user system within the mini program.
- Document address: https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/login.html

![](/resources/微信登陆.png)

## Token verification interceptor
- After the login is complete, all subsequent request headers carry the token.
- The TokenInterceptor interceptor verifies the token. If the token is forged or expired, it intercepts the request and responds to 401. If the token is valid, the token is released.
- After verifying the validity of the token, the user id is stored in UserThreadLocal and can be directly retrieved when needed.
- Methods in the Controller marked with @NoAuthorization do not intercept.

## General response principle
- To simplify development in Controller, responses are treated uniformly.
- If successful, it responds with 200 status code.
- If it fails, it responds with 500 status code.
- The response data is returned directly without the need for packaging.
- com.qingqi.interceptor.Com monResponseBodyAdvice class implement the specific logic.
