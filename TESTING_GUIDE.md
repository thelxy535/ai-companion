# 测试指南 - API 模型优化版本

**版本**: v2.1.0-beta.8
**日期**: 2026-08-14

---

## 📱 安装步骤

### 方法1：命令行安装（推荐）
```bash
cd D:/CC-Switch/cc-native-android
./gradlew installDebug
```

### 方法2：手动安装
1. 找到 APK 文件：
   ```
   D:\CC-Switch\cc-native-android\app\build\outputs\apk\debug\app-debug.apk
   ```
2. 拖拽到模拟器窗口安装

---

## 🧪 测试清单

### 1. ✅ 测试新增供应商

#### A. Anthropic (Claude)

**步骤**:
1. 打开设置 → AI 设置
2. 切换供应商到 "Anthropic (Claude)"
3. 输入 API Key（以 sk-ant- 开头）
4. 查看模型列表

**预期结果**:
- ✅ 显示 6 个 Claude 模型
- ✅ Claude-3.5-Sonnet-20241022
- ✅ Claude-3.5-Haiku-20241022
- ✅ Claude-3-Opus
- ✅ Claude-3-Sonnet
- ✅ Claude-3-Haiku

---

#### B. Google (Gemini)

**步骤**:
1. 切换到 "Google (Gemini)"
2. 输入 API Key
3. 查看模型列表

**预期结果**:
- ✅ 显示 6 个 Gemini 模型
- ✅ Gemini-2.0-Flash-Exp (最新)
- ✅ Gemini-1.5-Pro
- ✅ Gemini-1.5-Flash
- ✅ Gemini-1.5-Flash-8B

---

#### C. Mistral AI

**步骤**:
1. 切换到 "Mistral AI"
2. 输入 API Key
3. 查看模型列表

**预期结果**:
- ✅ 显示 7 个 Mistral 模型
- ✅ Mistral-Large-Latest
- ✅ Open-Mixtral-8x7B
- ✅ Codestral-Latest

---

#### D. 百度文心一言

**步骤**:
1. 切换到 "百度 (文心一言)"
2. 输入 API Key
3. 查看模型列表

**预期结果**:
- ✅ 显示 7 个文心模型
- ✅ ERNIE-4.0-8K
- ✅ ERNIE-3.5-8K
- ✅ ERNIE-Lite-8K

---

### 2. ✅ 测试更新的模型列表

#### OpenAI

**步骤**:
1. 切换到 "OpenAI"
2. 查看模型列表

**新增模型**:
- ✅ o1-preview（推理模型）
- ✅ o1-mini（推理模型）
- ✅ gpt-4o（最新旗舰）
- ✅ gpt-4o-mini（快速版本）

---

#### DeepSeek

**步骤**:
1. 切换到 "DeepSeek"
2. 查看模型列表

**新增模型**:
- ✅ deepseek-reasoner（推理模型）
- ✅ deepseek-chat
- ✅ deepseek-coder

---

#### 阿里云百炼

**步骤**:
1. 切换到 "阿里云百炼 (Qwen)"
2. 查看模型列表

**新增模型**:
- ✅ qwq-32b-preview（推理模型）
- ✅ qwen-max
- ✅ qwen2.5-72b-instruct
- ✅ qwen2.5-32b-instruct

---

### 3. ✅ 测试智能识别

#### A. 密钥识别

**测试用例**:

1. **Claude 密钥**
   - 输入：sk-ant-xxxxx
   - 预期：自动识别为 Anthropic

2. **OpenAI 密钥**
   - 输入：sk-xxxxx（40+字符）
   - 预期：识别为 OpenAI

3. **智谱AI 密钥**
   - 输入：32位字母数字
   - 预期：识别为智谱AI

---

#### B. URL 识别

**测试用例**:

1. **输入 URL**: https://api.anthropic.com/v1
   - 预期：自动选择 Anthropic

2. **输入 URL**: https://generativelanguage.googleapis.com/v1
   - 预期：自动选择 Google

3. **输入 URL**: https://api.mistral.ai/v1
   - 预期：自动选择 Mistral AI

---

### 4. ✅ 测试容错机制

#### A. API 加载失败

**步骤**:
1. 输入错误的 API Key
2. 尝试加载模型

**预期结果**:
- ✅ 不崩溃
- ✅ 自动降级到预定义模型列表
- ✅ 显示友好错误提示

---

#### B. 网络异常

**步骤**:
1. 断开网络
2. 切换供应商
3. 查看模型列表

**预期结果**:
- ✅ 显示预定义模型
- ✅ 不影响使用

---

### 5. ✅ 测试实际对话

#### A. 使用不同供应商

**测试流程**:
1. 配置 API Key
2. 选择模型
3. 发送测试消息
4. 验证回复

**推荐测试模型**:
- OpenAI: gpt-4o-mini
- Claude: claude-3-5-haiku
- Gemini: gemini-1.5-flash
- DeepSeek: deepseek-chat

---

#### B. 推理模型测试

**测试问题**:
```
请帮我解决这个数学问题：
一个数列的前三项是 2, 5, 10。
如果这是一个二次数列，第四项是多少？
请详细说明你的推理过程。
```

**测试模型**:
- OpenAI: o1-preview
- DeepSeek: deepseek-reasoner
- 阿里云: qwq-32b-preview

**预期**:
- ✅ 展示详细推理步骤
- ✅ 给出正确答案

---

## 📊 测试检查表

### 功能测试
- [ ] 新增供应商能正常选择
- [ ] 模型列表正确显示
- [ ] 智能识别正常工作
- [ ] 容错机制生效
- [ ] 实际对话正常

### 供应商测试
- [ ] OpenAI (10个模型)
- [ ] Anthropic (6个模型)
- [ ] Google (6个模型)
- [ ] 智谱AI (7个模型)
- [ ] DeepSeek (3个模型)
- [ ] 月之暗面 (3个模型)
- [ ] 阿里云 (12个模型)
- [ ] Mistral (7个模型)
- [ ] 百度 (7个模型)
- [ ] SiliconFlow (16个模型)

### 边界测试
- [ ] 错误的 API Key
- [ ] 网络断开
- [ ] 空 API Key
- [ ] 超长 API Key
- [ ] 特殊字符

---

## 🐛 已知问题

### 1. API 加载可能较慢
某些供应商的 /models 接口响应较慢，属正常现象。

### 2. 部分供应商使用预定义列表
以下供应商直接使用预定义模型（不调用 API）：
- Anthropic (Claude)
- Google (Gemini)
- 百度文心一言
- 智谱AI

---

## 📝 测试报告模板

### 测试环境
- 设备：Android 模拟器
- 系统：Android 9
- 版本：v2.1.0-beta.8

### 测试结果

#### 1. 新增供应商
- [ ] Anthropic: ✅ / ❌
- [ ] Google: ✅ / ❌
- [ ] Mistral: ✅ / ❌
- [ ] 百度: ✅ / ❌

#### 2. 模型列表
- [ ] OpenAI: ✅ / ❌
- [ ] DeepSeek: ✅ / ❌
- [ ] 阿里云: ✅ / ❌

#### 3. 智能识别
- [ ] 密钥识别: ✅ / ❌
- [ ] URL识别: ✅ / ❌

#### 4. 容错机制
- [ ] API失败: ✅ / ❌
- [ ] 网络异常: ✅ / ❌

#### 5. 实际对话
- [ ] 正常对话: ✅ / ❌
- [ ] 推理模型: ✅ / ❌

### 发现的问题
（记录任何问题）

### 建议
（记录改进建议）

---

## 🚀 快速测试脚本

如果你有可用的 API Key，可以按照以下顺序快速测试：

### 1分钟快速测试
```
1. 打开设置
2. 输入 OpenAI API Key
3. 查看模型列表（应该看到 o1-preview）
4. 选择 gpt-4o-mini
5. 发送一条测试消息
```

### 5分钟完整测试
```
1. 测试 OpenAI（包括 o1 系列）
2. 测试 Claude（如果有 API Key）
3. 测试 Gemini（如果有 API Key）
4. 测试智能识别
5. 测试容错机制
```

---

**准备就绪，开始测试！** 🧪

提示：如果 Bash 命令仍然不可用，请手动执行安装命令或拖拽 APK 到模拟器。
