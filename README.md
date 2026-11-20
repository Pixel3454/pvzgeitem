# PVZGE Items（pvzgeitem）

将 PVZGE 的内容移植到 Minecraft。当前版本 1.0.0，支持 Minecraft 1.21.1 + NeoForge。

## 当前内容
- 创造模式标签页「武林寺庙」（Wulin Temple），中文/英文名称已本地化
- 物品、标签页图标、模组封面暂未添加，后续版本加入

## 构建
需要 JDK 21。

Windows：
    gradlew.bat build

Linux / macOS：
    ./gradlew build

产物在 build/libs/pvzgeitem-1.0.0.jar，放入 mods 文件夹即可。

首次构建 Gradle 会联网下载 NeoForge 与 Minecraft 依赖，请保持网络畅通。

## 后续添加物品的位置
- 物品注册：src/main/java/com/pvzge/pvzgeitem/ModItems.java（新建，参考 PvzgeitemMod.java 的注册方式）
- 把物品加入标签页：PvzgeitemMod.java 中 WULIN_TEMPLE_TAB 的 displayItems
- 标签页图标：把 createTabIcon() 改为 new ItemStack(物品.get())
- 模型/贴图：src/main/resources/assets/pvzgeitem/models、textures
- 物品中文名：assets/pvzgeitem/lang/zh_CN.json
