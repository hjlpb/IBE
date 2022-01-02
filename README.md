# IBE
IBE implementation based on jpbc

# 身份基加密 (Identity based Encryption)算法

论文 [Identity-Based Encryption from the Weil Pairing](https://crypto.stanford.edu/~dabo/papers/bfibe.pdf)

## Setup

1. 生成pairing相关公共参数 $<e,G_1, G_T,Z_r>$
2. 选取随机数$x\in Z_r$ 作为系统主密钥$msk$
3. 选取随机元素$g\in G_1$作为生成元，计算公共参数$g^x$。因此，有系统公钥$pk=<g,g^x>$
4. 选取公共哈希函数 $H_1:\{0,1\}^*\rightarrow G_1^*$，$H_2:G_T \rightarrow \{0,1\}^n$

## KeyGen

1. 给定用户身份 $ID\in \{0,1\}^*$，将其映射为群$G_1$上的元素。即计算 $Q_{ID}=H_1(ID)$
2. 由系统主密钥$x$计算此$ID$对应的私钥为$sk=Q_{ID}^x$

## Encrypt

1. 针对目标用户身份$ID\in \{0,1\}^*$，计算 $Q_{ID}=H_1(ID)$
2. 选取随机数$r\in Z_r$，计算密文组件$C_1=g^r$ 
3. 计算$g_{ID}=e(Q_{ID},g^x)^r$
4. 计算密文组件$C_2=M \oplus H_2(g_{ID})$，其中$M \in \{0,1\}^n$是明文数据
5. 最终的密文为$<C_1,C_2>$

## Decrypt

1. 解密的关键在于恢复$g_{ID}$
2. $e(sk,C_1)=e(Q_{ID}^x,g^r)=e(Q_{ID},g)^{xr}=g_{ID}$
3. 恢复明文 $M=C_2 \oplus H_2(e(sk,C_1))$



# 代码实现



# 注意事项

1. 选择用Properties保存是因为支持键值读取，比如密文可能包含多个组件，方便分别读取每个组件。
2. SetProperties的第二个参数必须为String类型，对于Zr群元素x可以调用`x.toBigInteger().toString()`

