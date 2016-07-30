DexDiff
---
DexDiff通过反编译新旧dex文件后计算增量dex，并利用旧dex与增量dex生成新dex，可用于实现Android的热修复及增量更新。

原理
---
1. 基于dex2jar库反编译新旧dex		
2. 逐个对比新旧dex中的class：	
    - class仅存在于旧dex，保存至deleteClasses
    - class仅存在于新dex，保存至replaceClasses
    - class存在于新旧dex，但class数据不一致，保存新dex中的class至replaceClasses
3. 编译得到的replaceClasses得到replace.dex
4. 记录deleteClasses中的类签名得到delete.data
5. 根据replace.dex、delete.data以及旧dex便可以编译得到新dex

Dex2Jar
---
本项目依赖于[dex2jar](https://github.com/pxb1988/dex2jar)实现dex文件的编译及反编译，为了实现dexdiff对dex2jar做了如下改动：

- 修改部分字段为public权限
- 删除部分字段的final属性
- 重写部分类的hashCode、equals及toString方法
- 修改PackedSwitch及SparseSwitch为嵌套类

TODO
---
目前仅实现了class粒度上的对比，但这套方案完全可以实现method粒度甚至instruction粒度的对比

License
---
[Apache License 2.0](https://opensource.org/licenses/Apache-2.0)