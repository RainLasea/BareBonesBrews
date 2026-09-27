# KubeJS

脚本放在 `kubejs/server_scripts/`，`/reload` 生效。KubeJS 不是必装项。

## 写一条配方

配方类型已注册 KubeJS schema，可以直接用 `event.recipes`：

```js
ServerEvents.recipes(event => {
  event.recipes.barebonesbrews.rough_brewing(
    Item.of('barebonesbrews:rough_potion', {
      'barebonesbrews:source_potion': 'minecraft:strength',
      'minecraft:potion_contents': {
        custom_effects: [{ id: 'minecraft:strength', amplifier: 0, duration: 1800 }]
      }
    }),
    ['#c:mushrooms', 'minecraft:blaze_powder']
  ).duration(300).id('my_pack:rough_strength')
})
```

| 参数 | 说明 |
|---|---|
| 第一个 | 产出物品栈 |
| 第二个 | 材料数组，最多 5 个 |
| `.duration(n)` | 煮制刻数，默认 400 |
| `.id(...)` | 配方 ID，省略时由 KubeJS 生成 |

产出物品需要 `barebonesbrews:source_potion` 和 `minecraft:potion_contents` 两个组件，格式见[自定义配方](自定义配方)。

## 改现有配方

```js
ServerEvents.recipes(event => {
  event.remove({ id: 'barebonesbrews:rough_brewing/potion/minecraft/strength' })

  event.replaceInput(
    { id: 'barebonesbrews:rough_brewing/potion/minecraft/swiftness' },
    'minecraft:sugar', 'minecraft:honey_bottle'
  )
})
```

改产出用 `event.replaceOutput`。ID 见[自定义内容](自定义内容)。

装了 Hexalia 时前缀是 `hexalia_rough/`。分开写两套：

```js
ServerEvents.recipes(event => {
  if (Platform.isLoaded('hexalia')) {
    // Hexalia 的改动
  } else {
    // 原版炼药锅的改动
  }
})
```

## 改热源标签

```js
ServerEvents.tags('block', event => {
  event.add('barebonesbrews:heat_sources', 'minecraft:diamond_block')
  event.remove('barebonesbrews:heat_sources', 'minecraft:magma_block')
})
```

标签在匹配时实时读取，`/reload` 后生效。

配置项 `cauldron_heat_sources` 被修改后标签不再被读取，两者只用其中一个。

## 写入 Hexalia 的小炼药锅

```js
ServerEvents.recipes(event => {
  if (!Platform.isLoaded('hexalia')) return

  event.custom({
    type: 'hexalia:small_cauldron',
    ingredients: [{ item: 'minecraft:amethyst_shard' }],
    result: {
      id: 'barebonesbrews:rough_potion',
      count: 1,
      components: {
        'barebonesbrews:source_potion': 'minecraft:strength',
        'minecraft:potion_contents': {
          custom_effects: [{ id: 'minecraft:strength', amplifier: 0, duration: 1800 }]
        }
      }
    },
    duration: 200
  }).id('my_pack:hexalia_strength')
})
```

材料最多 4 个。

## 示例

`examples/kubejs/server_scripts/` 下有两份脚本，分别对应原版炼药锅和 Hexalia。
