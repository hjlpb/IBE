import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;


public class JPBCDemo {
    public static void main(String[] args) {
        // 一、基于特定椭圆曲线类型生成Pairing实例
        // 1.从文件导入椭圆曲线参数
        Pairing bp = PairingFactory.getPairing("a.properties");

        // 2.自定义曲线参数
        // int rBits = 160;
        // int qBits = 512;
        // TypeACurveGenerator pg = new TypeACurveGenerator(rBits, qBits);
        // PairingParameters pp = pg.generate();
        // Pairing bp = PairingFactory.getPairing(pp);

        // 二、选择群上的元素
        Field G1 = bp.getG1();
        Field Zr = bp.getZr();
        Element g = G1.newRandomElement().getImmutable();
        Element a = Zr.newRandomElement().getImmutable();
        Element b = Zr.newRandomElement().getImmutable();

        // 三、计算等式左半部分
        Element ga = g.powZn(a);
        Element gb = g.powZn(b);
        Element egg_ab = bp.pairing(ga,gb);

        // 四、计算等式右半部分
        Element egg = bp.pairing(g,g).getImmutable();
        Element ab = a.mul(b);
        Element egg_ab_p = egg.powZn(ab);

        if (egg_ab.isEqual(egg_ab_p)) {
            System.out.println("yes");
        }
        else {
            System.out.println("No");
        }
    }
}
