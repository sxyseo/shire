---
layout: default
title: Shire Environment
parent: Shire Language
nav_order: 9
---

Shire Environment 用于定义 Shire 的环境变量，用于存储一些敏感信息。 使用方式 `.shireEnv.json` 文件来存储环境变量，Shire
将会自动加载这种文件。

当前 Shire Env 支持两种变量：

- `development`：配置 Token、API Key 等信息。
- `models`：配置模型信息（在 `0.7.3` 版本后支持）。

## `.shireEnv.json` 文件

`.shireEnv.json` 用于存储环境变量，Shire 将会自动加载这种文件，当前只支持 `development` 环境。

```json
{
  "development": {
    "apiKey": "xxx"
  },
  "models": [
    {
      "title": "quickModel",
      "apiKey": "sk-xxx",
      "model": "gpt-4o-mini",
      "temperature": 0.3
    },
    {
      "title": "gpt4o",
      "apiKey": "sk-xxx",
      "model": "gpt-4o"
    },
    {
      "title": "glm-4-plus",
      "apiKey": "xxx",
      "model": "glm-4-plus",
      "apiBase": "https://open.bigmodel.cn/api/paas/v4/chat/completions"
    }
  ]
}
```