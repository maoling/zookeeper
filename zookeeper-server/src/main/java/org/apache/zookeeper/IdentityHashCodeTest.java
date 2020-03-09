package org.apache.zookeeper;

public class IdentityHashCodeTest {
	public static void main(String[] args) {
		/*通过identityHashCode获取对象唯一标识，只要不是同一对象则返回值必定不同*/
		String str1 = new String("java");//定义内容相同的对象，String类根据字符串内容得出hashcode，但是identityhashcode根据地址计算
		String str2 = new String("java");
		System.out.println("hashcode: " + "str1.hashcode = " + str1.hashCode() + " , str2.hashcode = " + str2.hashCode());
		System.out.println("identityhashcode: " + "str1 = " + System.identityHashCode(str1) + " , str2 = " + System.identityHashCode(str2));
		String str3 = "caikai";//直接定义的字符串存储在常量池，重复定义将指向同一个字符串（地址相同）
		String str4 = "caikai";
		System.out.println("str3 = " + System.identityHashCode(str3) + " , str4 = " + System.identityHashCode(str4));
	}
}