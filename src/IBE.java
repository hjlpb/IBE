import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Properties;

public class IBE {

    public static void setup(String pairingParametersFileName, String pkFileName, String mskFileName) {
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);

        Element x = bp.getZr().newRandomElement().getImmutable();
        Properties mskProp = new Properties();
        //mskProp.setProperty("x", x.toBigInteger().toString());   //x有toBigInteger方法，因此可以用这种方式，但g不能
        //后面对写的元素统一采用如下方法：首先将元素转为字节数组，然后进行Base64编码为可读字符串
        mskProp.setProperty("x", Base64.getEncoder().encodeToString(x.toBytes()));
        storePropToFile(mskProp, mskFileName);

        Element g = bp.getG1().newRandomElement().getImmutable();
        Element gx = g.powZn(x).getImmutable();
        Properties pkProp = new Properties();
        //pkProp.setProperty("g", new String(g.toBytes()));  //可以用这种方式将g转换为字符串后写入，但文件中显示乱码
        //为了避免乱码问题，采用Base64编码方式
        pkProp.setProperty("g", Base64.getEncoder().encodeToString(g.toBytes()));
        pkProp.setProperty("gx", Base64.getEncoder().encodeToString(gx.toBytes()));
        storePropToFile(pkProp, pkFileName);
    }

    public static void keygen(String pairingParametersFileName, String id, String mskFileName, String skFileName) throws NoSuchAlgorithmException {
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);

        byte[] idHash = sha1(id);
        Element QID = bp.getG1().newElementFromHash(idHash, 0, idHash.length).getImmutable();

        Properties mskProp = loadPropFromFile(mskFileName);
        String xString = mskProp.getProperty("x");
//        Element x = bp.getZr().newElement(new BigInteger(xString));  //对应于前面的x.toBigInteger().toString()方式
        Element x = bp.getZr().newElementFromBytes(Base64.getDecoder().decode(xString)).getImmutable();  //Base64编码后对应的恢复元素的方法

        Element sk = QID.powZn(x).getImmutable();
        Properties skProp = new Properties();
        skProp.setProperty("sk", Base64.getEncoder().encodeToString(sk.toBytes()));
        storePropToFile(skProp, skFileName);
    }

    public static void encrypt(String pairingParametersFileName, String message, String id, String pkFileName, String ctFileName) throws NoSuchAlgorithmException {
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);

        byte[] idHash = sha1(id);
        Element QID = bp.getG1().newElementFromHash(idHash, 0, idHash.length).getImmutable();

        Properties pkProp = loadPropFromFile(pkFileName);
        String gString = pkProp.getProperty("g");
        Element g = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(gString)).getImmutable();
        String gxString = pkProp.getProperty("gx");
        Element gx = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(gxString)).getImmutable();

        Element r = bp.getZr().newRandomElement().getImmutable();
        Element C1 = g.powZn(r).getImmutable();

        Element gID = bp.pairing(QID, gx).powZn(r).getImmutable();

        String gIDString = new String(gID.toBytes());
        byte[] HgID = sha1(gIDString);
        byte[] messageByte = message.getBytes();

        byte[] C2 = new byte[messageByte.length];
        //假设m明文字节长度n小于HgID的长度20，取HgID的前n个字节进行异或
        for (int i = 0; i < messageByte.length; i++){
            C2[i] = (byte)(messageByte[i] ^ HgID[i]);
        }

        Properties ctProp = new Properties();
        ctProp.setProperty("C1", Base64.getEncoder().encodeToString(C1.toBytes()));
        ctProp.setProperty("C2", Base64.getEncoder().encodeToString(C2));
        storePropToFile(ctProp, ctFileName);
    }

    public static String decrypt(String pairingParametersFileName, String ctFileName, String pkFileName, String skFileName) throws NoSuchAlgorithmException {
        Pairing bp = PairingFactory.getPairing(pairingParametersFileName);

        Properties skProp = loadPropFromFile(skFileName);
        String skString = skProp.getProperty("sk");
        Element sk = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(skString)).getImmutable();

        Properties ctProp = loadPropFromFile(ctFileName);
        String C1String = ctProp.getProperty("C1");
        Element C1 = bp.getG1().newElementFromBytes(Base64.getDecoder().decode(C1String)).getImmutable();
        String C2String = ctProp.getProperty("C2");
        byte[] C2 = Base64.getDecoder().decode(C2String);

        Element gID = bp.pairing(sk, C1).getImmutable();

        String gIDString = new String(gID.toBytes());
        byte[] HgID = sha1(gIDString);
        byte[] res = new byte[C2.length];
        for (int i = 0; i < C2.length; i++){
            res[i] = (byte)(C2[i] ^ HgID[i]);
        }
        return new String(res);
    }

    public static void storePropToFile(Properties prop, String fileName){
        try(FileOutputStream out = new FileOutputStream(fileName)){
            prop.store(out, null);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.println(fileName + " save failed!");
            System.exit(-1);
        }
    }

    public static Properties loadPropFromFile(String fileName) {
        Properties prop = new Properties();
        try (FileInputStream in = new FileInputStream(fileName)){
            prop.load(in);
        }
        catch (IOException e){
            e.printStackTrace();
            System.out.println(fileName + " load failed!");
            System.exit(-1);
        }
        return prop;
    }

    public static byte[] sha1(String content) throws NoSuchAlgorithmException {
        MessageDigest instance = MessageDigest.getInstance("SHA-1");
        instance.update(content.getBytes());
        return instance.digest();
    }

    public static void main(String[] args) throws Exception {

        String idBob = "bob@example.com";
        String idAlice = "alice@example.com";
        String message = "i hate you";

        String dir = "data/";
        String pairingParametersFileName = "a.properties";
        String pkFileName = dir + "pk.properties";
        String mskFileName = dir + "msk.properties";
        String skFileName = dir + "sk.properties";
        String ctFileName = dir + "ct.properties";

        setup(pairingParametersFileName, pkFileName, mskFileName);

        keygen(pairingParametersFileName, idBob, mskFileName, skFileName);

        encrypt(pairingParametersFileName, message, idBob, pkFileName, ctFileName);

        String res = decrypt(pairingParametersFileName, ctFileName, pkFileName, skFileName);

        System.out.println(res);
    }

}
