# ICS 投递助手（Cloudflare Worker）

## 为什么需要它

网页版用 Blob 下载 `.ics` 时，iOS 会把文件当成 `application/octet-stream`，
存进「文件」App 后不会和日历建立关联——于是**既没有「添加到日历」按钮，共享菜单里也不出现日历**。

iOS 只认 `Content-Type: text/calendar`，而这个响应头必须由服务器给出。
GitHub Pages 是纯静态托管，没法按请求生成响应头，所以需要一个几行的动态服务。

这个 Worker 只做一件事：把 URL 里携带的 ICS 内容解码，加上正确的 `Content-Type` 返回。
**不存储任何数据**，解码完原样返回，进程结束即丢弃。

## 部署（约 5 分钟，免费）

1. 注册 **https://dash.cloudflare.com/sign-up**（邮箱即可，不用信用卡，不用绑卡）
2. 左侧 **Workers & Pages** → **Create** → **Create Worker**
3. 名字填 `birthday-ics`，点 **Deploy**（会先部署一个 Hello World）
4. 点 **Edit code**，把编辑器里默认的代码全部删掉，粘贴本目录 `worker.js` 的全部内容
5. 右上角 **Deploy**
6. 复制你的地址，形如：
   ```
   https://birthday-ics.你的子域.workers.dev
   ```

把上面这个地址发给我，我把它接进网页版。

## 免费额度

每天 10 万次请求，个人使用永远用不完。

## 调用方式

```
https://birthday-ics.xxx.workers.dev/?d=<base64url>&n=birthday-1114.ics
```

- `d`：`buildIcs()` 生成的文本，UTF-8 编码后转 base64url
- `n`：下载时的文件名（只允许字母数字、下划线、点、横线）

## 顺带的好处：可以订阅

有了这个地址，还能让日历**订阅**这套提醒（`webcal://` 开头），
订阅的内容会自动刷新——以后改了好友信息，日历会自动跟着变，不用重新导入。
