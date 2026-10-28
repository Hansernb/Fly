import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

// https://vitejs.dev/config/

const here = path.dirname(fileURLToPath(import.meta.url))

/**
 * 动态解析后端端口：读后端【激活 profile】的 server.port，
 * 这样后端改端口（application-dev.properties）后，前端代理自动跟随，无需改本文件。
 */
function resolveBackendPort() {
  const fallback = 8080
  try {
    const root = path.resolve(here, '..')
    // 找到后端模块（同级目录下含 pom.xml 的文件夹）
    const beDir = fs
      .readdirSync(root, { withFileTypes: true })
      .filter((d) => d.isDirectory())
      .map((d) => path.join(root, d.name))
      .find((dir) => fs.existsSync(path.join(dir, 'pom.xml')))
    if (!beDir) return fallback

    const resDir = path.join(beDir, 'src', 'main', 'resources')
    const mainFile = path.join(resDir, 'application.properties')

    // 1) 读取激活的 profile（默认 dev）
    let profile = 'dev'
    if (fs.existsSync(mainFile)) {
      const m = fs
        .readFileSync(mainFile, 'utf8')
        .match(/^[ \t]*spring\.profiles\.active[ \t]*=[ \t]*(\S+)/m)
      if (m) profile = m[1].trim()
    }

    // 2) 优先读激活 profile 的 server.port，其次 application.properties
    for (const f of [path.join(resDir, `application-${profile}.properties`), mainFile]) {
      if (!fs.existsSync(f)) continue
      const m = fs.readFileSync(f, 'utf8').match(/^[ \t]*server\.port[ \t]*=[ \t]*(\d+)/m)
      if (m) return Number(m[1])
    }
  } catch (e) {
    // 解析失败则用默认端口，不影响启动
  }
  return fallback
}

const backendPort = resolveBackendPort()

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    open: true,
    proxy: {
      '/api': `http://localhost:${backendPort}`
    }
  }
})
