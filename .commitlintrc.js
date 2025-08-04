const fs = require('fs')
const path = require('path')
const {
  execSync
} = require('child_process')

// 扫描含 build.gradle(.kts) 的模块，安卓项目识别
function findModules(dir, result = []) {
  const entries = fs.readdirSync(dir, {
    withFileTypes: true
  })
  for(const entry of entries) {
    if(entry.name.startsWith('.')) continue

    const fullPath = path.join(dir, entry.name)
    if(entry.isDirectory()) {
      const hasGradle = fs.existsSync(path.join(fullPath, 'build.gradle')) ||
        fs.existsSync(path.join(fullPath, 'build.gradle.kts'))

      if(hasGradle) {
        const relPath = path.relative(__dirname, fullPath)
          .replace(/\\/g, '/')
        result.push(relPath)
      } else {
        findModules(fullPath, result)
      }
    }

  }
  return result
}

// 获取被修改模块名，格式化为 cz-git 的 scope 格式
function getChangedScopes(modules) {
  const changed = new Set()
  try {
    const output = execSync('git status --porcelain', {
      encoding: 'utf-8'
    })
    const files = output.split('\n')
      .map(l => l.trim()
        .split(/\s+/)
        .pop())

    for(const file of files) {
      for(const mod of modules) {
        if(file && file.startsWith(mod + '/')) {
          changed.add(mod)
        }
      }
    }

  } catch (_) {}

  return [...changed]
}

const allModules = findModules(path.resolve(__dirname))
const changedScopes = getChangedScopes(allModules)

/** @type {import('cz-git').UserConfig} */
module.exports = {
  rules: {
        // @see: https://commitlint.js.org/#/reference-rules
  },
  
  prompt: {
    messages: {
            type: '选择你要提交的类型 :',
            scope: '选择一个提交范围（可选）:',
            customScope: '请输入自定义的提交范围 :',
            subject: '填写简短精炼的变更描述 :\n',
            body: '填写更加详细的变更描述（可选）。使用 "|" 换行 :\n',
            breaking: '列举非兼容性重大的变更（可选）。使用 "|" 换行 :\n',
            footerPrefixesSelect: '选择关联issue前缀（可选）:',
            customFooterPrefix: '输入自定义issue前缀 :',
            footer: '列举关联issue (可选) 例如: #31, #I3244 :\n',
            confirmCommit: '是否提交或修改commit ?',
        },
        types: [
            { value: 'feat', name: 'feat:     新增功能 | A new feature' },
            { value: 'fix', name: 'fix:      修复缺陷 | A bug fix' },
            { value: 'docs', name: 'docs:     文档更新 | Documentation only changes' },
            { value: 'style', name: 'style:    代码格式 | Changes that do not affect the meaning of the code' },
            { value: 'refactor', name: 'refactor: 代码重构 | A code change that neither fixes a bug nor adds a feature' },
            { value: 'perf', name: 'perf:     性能提升 | A code change that improves performance' },
            { value: 'test', name: 'test:     测试相关 | Adding missing tests or correcting existing tests' },
            { value: 'build', name: 'build:    构建相关 | Changes that affect the build system or external dependencies' },
            { value: 'ci', name: 'ci:       持续集成 | Changes to our CI configuration files and scripts' },
            { value: 'revert', name: 'revert:   回退代码 | Revert to a commit' },
            { value: 'chore', name: 'chore:    其他修改 | Other changes that do not modify src or test files' },
        ],
        allowBreakingChanges: ['feat', 'fix', 'build'],
        scopes: allModules,
        defaultScope: changedScopes,
        // customScopesAlign: changedScopes.length === 0 ? 'top-bottom' : 'bottom',
        enableMultipleScopes: true,
        scopeEnumSeparator: ",",
        markBreakingChangeMode: true
  },
}