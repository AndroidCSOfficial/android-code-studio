# 项目说明

该项目主要适配`AndroidIDE`的自举，目前已经可以在`AndroidIDE`的终端环境成功完成构建过程。

> [!WARNING]
> 可以在`AndroidIDE`中直接打开该项目，但你需要关闭`LogSender`选项。并且构建过程可能会因为资源占用过大导致卡死，总之，`dev`分支不建议直接在`AndroidIDE`中打开。

# 构建环境

- jdk: jdk-17
- compileSdk: 34
- buildTools: 34.0.4
- gradle: 8.8

# 构建前提

## buildTools

请确保你已经安装了`34.0.4`版本的`build-tools`

## platforms/android-34

请确保你已经安装了`34`版本的`platform`资源。

你可以使用如下命令进行安装：
```shell
sdkmanager "platforms;android-34"
```

## gradlew

如果你在`AndroidIDE`的终端环境进行构建，建议你在`~/.bashrc`文件中添加一个如下的函数：
```shell
# In $HOME/.bashrc file

function gradlew {
    file="./gradlew"
    if test -f "$file" ; then
        bash $file -Pandroid.aapt2FromMavenOverride=$HOME/.androidide/aapt2 $@
    else
        echo "Invoke this command from a project's root directory."
    fi
}

export -f gradlew
```

然后你就可以使用`gradlew`替代`./gradlew -Pandroid.aapt2FromMavenOverride=$HOME/.androidide/aapt2`命令了。

# 签名配置

你需要修改根目录`local.properties`文件中的签名配置选项，至少需要修改`signing.storeFile`指向签名的实际位置，这取决于你的项目路径。

配置文件示例如下：
```properties
# signing conf
signing.storeFile=/storage/emulated/0/AndroidIDEProjects/AndroidIDE-R/signing/signing-key.jks
signing.storePassword=123456
signing.keyAlias=android-ide
signing.keyPassword=123456
```

# 开始构建

你需要在项目根目录执行如下的命令：
```shell
gradlew :core:app:assembleRelease 2>&1 | tee ./build.log
```

如果构建失败，你可以查看`build.log`查找原因。