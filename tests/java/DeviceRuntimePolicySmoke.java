import dev.mobile.runelite.DeviceRuntimePolicy;
public class DeviceRuntimePolicySmoke {
    public static void main(String[] args) {
        long gib=1024L*1024*1024;
        check(384,2*gib,false,true);
        check(512,3*gib,false,true);
        check(512,4*gib,false,true);
        check(768,8*gib,false,true);
        check(384,8*gib,true,true);
        check(384,8*gib,false,false);
        check(768,0,false,true);
        System.out.println("DEVICE_RUNTIME_MEMORY_POLICY_OK");
    }
    static void check(int expected,long total,boolean low,boolean wide) {
        if(DeviceRuntimePolicy.heapMiB(total,low,wide)!=expected) throw new AssertionError("Memory budget exceeded");
    }
}
