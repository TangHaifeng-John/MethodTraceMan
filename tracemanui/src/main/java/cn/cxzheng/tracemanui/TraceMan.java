package cn.cxzheng.tracemanui;

import android.os.Build;
import android.os.Looper;
import android.os.Trace;
import android.support.annotation.RequiresApi;
import android.util.Log;
import com.ctrip.ibu.hotel.debug.server.producer.module.methodcost.MethodInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Create by cxzheng on 2019/8/26
 * 全方法插桩内容
 */
public class TraceMan {

    private static CopyOnWriteArrayList<Entity> methodList = new CopyOnWriteArrayList();

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void start(String name) {
        Trace.beginSection(name);
        Log.i("hf","开始执行方法:"+name);
        if (isOpenTraceMethod()) {
            methodList.add(new Entity(name, System.currentTimeMillis(), true, isInMainThread()));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void end(String name) {
        Trace.endSection();
        Log.i("hf","结束执行方法:"+name);
        if (isOpenTraceMethod()) {
            methodList.add(new Entity(name, System.currentTimeMillis(), false, isInMainThread()));
        }
    }


    public static void startCollectMethodCost() {
        resetTraceManData();
    }

    public static List<MethodInfo> endCollectMethodCost() {
        List<MethodInfo> resultList = obtainMethodCostData();
        resetTraceManData();
        return resultList;
    }


    public static void resetTraceManData() {
        methodList.clear();
    }

    /**
     * 处理插桩数据，按顺序获取所有方法耗时
     */
    public static List<MethodInfo> obtainMethodCostData() {
        List<MethodInfo> resultList = new ArrayList();
        for (int i = 0; i < methodList.size(); i++) {
            Entity startEntity = methodList.get(i);
            if (!startEntity.isStart) {
                continue;
            }
            startEntity.pos = i;
            Entity endEntity = findEndEntity(startEntity.name, i + 1);

            if (startEntity != null && endEntity != null && endEntity.time - startEntity.time > 0) {
                resultList.add(createMethodInfo(startEntity, endEntity));
            }
        }

        for (int i = 0; i < resultList.size(); i++) {
            MethodInfo methodInfo = resultList.get(i);
            String threadText = methodInfo.isMainThread() ? "[主线程]" : "[非主线程]";
            Log.i("costTime", methodInfo.getName() + "  " + methodInfo.getCostTime() + "ms" + " " + threadText);
        }

        return resultList;
    }

    /**
     * 找到方法对应的结束点
     *
     * @param name
     * @param startPos
     * @return
     */
    private static Entity findEndEntity(String name, int startPos) {
        int sameCount = 1;
        for (int i = startPos; i < methodList.size(); i++) {
            Entity endEntity = methodList.get(i);
            if (endEntity.name.equals(name)) {
                if (endEntity.isStart) {
                    sameCount++;
                } else {
                    sameCount--;
                }
                if (sameCount == 0 && !endEntity.isStart) {
                    endEntity.pos = i;
                    return endEntity;
                }
            }
        }
        return null;
    }

    private static MethodInfo createMethodInfo(Entity startEntity, Entity endEntity) {
        return new MethodInfo(startEntity.name,
                endEntity.time - startEntity.time, startEntity.pos, endEntity.pos, startEntity.isMainThread);
    }


    private static boolean isInMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private static boolean isOpenTraceMethod() {
        return MethodTraceServerManager.isActiveTraceMan;
    }

    static class Entity {
        public String name;
        public Long time;
        public boolean isStart;
        public int pos;
        public boolean isMainThread;

        public Entity(String name, Long time, boolean isStart, boolean isMainThread) {
            this.name = name;
            this.time = time;
            this.isStart = isStart;
            this.isMainThread = isMainThread;
        }
    }

}
