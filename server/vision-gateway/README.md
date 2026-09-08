# SYLORA Vision Gateway

受控的本机 FastAPI 视觉网关。服务只应监听 `127.0.0.1:8787`，只向本机 Ollama 的固定接口发送视觉分析请求，不是通用代理。

## 安全边界

- `POST /v1/pair` 接受一次性短期配对码和安装标识；数据库只保存配对码哈希和设备 token 哈希。
- `POST /v1/vision/analyze` 仅接受 `Authorization: Bearer <device-token>`。设备可由管理 CLI 撤销；同一安装重新配对会撤销旧设备。
- 请求体传输上限为 6 MiB，以容纳 Base64 编码后的最大 4 MiB 二进制图片；解码后的图片仍严格限制为 4 MiB。图片仅允许 JPEG、PNG、WebP，并同时校验声明 MIME、文件签名、Pillow 解码结果和 16,000,000 像素上限；动画图片被拒绝。
- 用户文本最多 2,000 字符；最多 3 条上下文，每条最多 1,000 字符。
- 单进程内最多一个运行中的推理和一个等待中的推理；再来的请求返回 429。每个设备 token 另有滑动窗口限速。
- 上游固定为 `http://127.0.0.1:11434/api/chat` 与 `qwen2.5vl:3b`。上游超时返回 504，其他上游不可用或格式错误返回 502。
- 日志只包含请求 ID、方法、受控路径、状态码和非敏感错误分类；不会记录 token、配对码、图片 Base64、完整用户文本或 Ollama 错误正文。
- `/health` 只返回 `{"status":"ok"}`，不暴露版本、模型、数据库或凭据状态。

## API 形状

配对请求体为 `{"pairing_code":"<ONE_TIME_CODE>","installation_id":"<INSTALLATION_ID>"}`。成功响应只在该次响应中返回设备 Bearer token；响应带 `Cache-Control: no-store`。

视觉请求必须使用 `Authorization: Bearer <DEVICE_TOKEN>` 和 Android 固定发送的 JSON 体：

```json
{
  "image_base64": "<BASE64_IMAGE>",
  "mime_type": "image/png",
  "user_text": "describe this image",
  "conversation_context": ["optional prior context"]
}
```

成功响应严格为一个字符串字段 `{"analysis":"..."}`。该字符串恒为六行，字段依次是 `主要对象`、`环境`、`动作`、`文字`、`氛围`、`语境理解`；任何上游原始响应都不会转发。

## 本地运行

需要 Ubuntu、Python 3.11 和已运行的本机 Ollama（模型准备由运维流程负责）。本项目不会自动联网安装依赖：

```bash
cd /opt/cc-vision-gateway
python3.11 -m venv .venv
.venv/bin/python -m pip install --no-index --find-links /opt/cc-wheelhouse -r requirements.txt
```

`requirements.txt` 锁定运行时及直接依赖闭包的精确版本。上例使用预先审计的本地 wheel 缓存，避免生产部署阶段隐式联网；如果使用组织内部制品源，必须在安装前完成制品审计。

设置运行目录和 pepper。pepper 必须是至少 32 个字符的随机值，并且不能使用示例值：

```bash
umask 077
install -d -m 700 /data/cc-vision-gateway
install -d -m 750 /etc/cc-vision-gateway
cp .env.example /etc/cc-vision-gateway/vision-gateway.env
chmod 600 /etc/cc-vision-gateway/vision-gateway.env
# 编辑 vision-gateway.env，写入随机 CC_VISION_TOKEN_PEPPER
```

手动开发启动（仍然只监听 loopback）：

```bash
CC_VISION_DATA_DIR=/data/cc-vision-gateway \
CC_VISION_TOKEN_PEPPER='<RANDOM_PEPPER_FROM_SECRET_STORE>' \
.venv/bin/uvicorn app.main:app --host 127.0.0.1 --port 8787 --workers 1 --no-access-log
```

不要增加 Uvicorn worker：全局推理闸门是进程内状态，多个 worker 会破坏“一个运行 + 一个排队”的约束。

## 管理 CLI

CLI 与服务使用同一个环境文件和 SQLite 数据目录。命令输出中的配对码只用于一次配对，必须通过受控的本地管理通道交给设备；不要写入 shell 历史、工单、日志或聊天记录。

```bash
set -a; . /etc/cc-vision-gateway/vision-gateway.env; set +a
.venv/bin/python -m app.admin generate-code --ttl-seconds 600
.venv/bin/python -m app.admin list-devices
.venv/bin/python -m app.admin revoke-device '<DEVICE_ID_FROM_LIST>'
```

配对码有效期范围是 1 到 3,600 秒，默认 600 秒且只能成功消费一次。设备配对成功后，API 会返回一次 Bearer token；客户端必须安全保存，服务端不会保存明文副本。

## systemd 安装

以下步骤假设代码部署到 `/opt/cc-vision-gateway`，运行用户和组为 `ccvision`，数据库目录为 `/data/cc-vision-gateway`：

```bash
sudo install -d -o ccvision -g ccvision -m 750 /opt/cc-vision-gateway
sudo install -d -o ccvision -g ccvision -m 700 /data/cc-vision-gateway
sudo install -d -m 750 /etc/cc-vision-gateway
sudo install -m 640 systemd/cc-vision-gateway.service /etc/systemd/system/cc-vision-gateway.service
sudo systemctl daemon-reload
sudo systemctl enable --now cc-vision-gateway.service
sudo systemctl status cc-vision-gateway.service
curl --fail http://127.0.0.1:8787/health
```

服务单元配置了 `NoNewPrivileges`, `PrivateTmp`, `ProtectSystem=strict`, `ProtectHome` 和仅允许数据库目录写入。应用自身仍必须运行在非 root 用户下。

## Caddy 模板

`Caddyfile.template` 仅是反向代理模板，不是自动暴露配置。部署前必须替换示例站点名，确认网络 ACL、TLS 和认证边界，并保持上游为 `127.0.0.1:8787`。模板显式丢弃 Caddy access log，设置 6 MiB Base64 传输限制，并把上游响应头等待时间设为 270 秒，避免 CPU 推理被反代过早中止；这不能替代应用层的 4 MiB 解码图片限制。

## 回滚

回滚目标是上一份经过验证的应用目录，不删除状态数据库：

```bash
sudo systemctl stop cc-vision-gateway.service
sudo mv /opt/cc-vision-gateway /opt/cc-vision-gateway.failed
sudo mv /opt/cc-vision-gateway.previous /opt/cc-vision-gateway
sudo systemctl start cc-vision-gateway.service
curl --fail http://127.0.0.1:8787/health
sudo journalctl -u cc-vision-gateway.service -n 100 --no-pager
```

如果回滚版本需要数据库迁移，应先制作 `/data/cc-vision-gateway` 的离线备份并遵循该版本的兼容性说明；不要复制或打印数据库中的哈希。确认新版本启动、健康检查和受控配对/撤销流程后，再清理失败目录。Caddy 配置变更应单独执行 `caddy validate` 后再 reload，失败时恢复上一份 Caddy 配置。

## 本地验证

在已安装锁定依赖的环境中运行：

```bash
python -m compileall -q app tests
pytest
```

测试不会调用真实 Ollama；API 测试替换为内存中的假客户端，并覆盖配对一次性消费、token 撤销、输入边界、图片签名/解码、日志脱敏、限速和推理队列。
