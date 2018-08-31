package work;

import bottom.BottomMonitor;
import bottom.BottomService;
import bottom.Task;
import main.Schedule;

import java.io.IOException;

/**
 *
 * 注意：请将此类名改为 S+你的学号   eg: S161250001
 * 提交时只用提交此类和说明文档
 *
 * 在实现过程中不得声明新的存储空间（不得使用new关键字，反射和java集合类）
 * 所有新声明的类变量必须为final类型
 * 不得创建新的辅助类
 *
 * 可以生成局部变量
 * 可以实现新的私有函数
 *
 * 可用接口说明:
 *
 * 获得当前的时间片
 * int getTimeTick()
 *
 * 获得cpu数目
 * int getCpuNumber()
 *
 * 对自由内存的读操作  offset 为索引偏移量， 返回位置为offset中存储的byte值
 * byte readFreeMemory(int offset)
 *
 * 对自由内存的写操作  offset 为索引偏移量， 将x写入位置为offset的内存中
 * void writeFreeMemory(int offset, byte x)
 *
 */
public class S161250059 extends Schedule{


    /**
     * 上一个任务的存放地址
     */
    private static final int formerTaskFA =0;

    /**
     *cpu状态起始地址
     */

    private static final int cpuStateBeginner = formerTaskFA + 4;


    /**
     * 资源位示图开始地址
     */
    private static final int resourceTableFA =cpuStateBeginner+20;

    /**
     * pcb寻址表起始地址
     */
    private static final int addressTableFA =resourceTableFA+128;

    /**
     * pcb存储空间起始地址
     */
    private static final int storageFA =addressTableFA+4000;

    /**
     * pcb任务id首地址
     */
    private static final int taskIDFA = 0;

    /**
     * pcb任务到达时间首地址
     */
    private static final int taskArriveTimeFA = 4;


    /**
     * pcb任务cpu占用时间首地址
     */
    private static final int taskRequireTimeFA = 8;

    /**
     * pcb任务剩余时间首地址
     */
    private static final int taskRemainTimeFA = 12;

    /**
     * pcb任务资源长度首地址
     */
    private static final int taskResourceLengthFA = 16;

    /**
     * pcb任务资源首地址
     */
    private static final int taskResourceFA = 20;


    /**
     *
     * @param arrivedTask 到达任务数组， 数组长度不定
     * @param cpuOperate  （返回值）cpu操作数组  数组长度为cpuNumber
     *                    cpuOperate[0] = 1 代表cpu0在当前时间片要执行任务1
     *                    cpuOperate[1] = 2 代表cpu1在当前时间片要执行任务2
     */
    @Override
    public void ProcessSchedule(Task[] arrivedTask, int[] cpuOperate) {


        if(arrivedTask != null){
            for(int i=0; i<arrivedTask.length; i++){
                int time = getTimeTick();
                initProcess(arrivedTask[i], time);
            }
        }
        getReady();
        int CPUNumber = getCpuNumber()-1;
        int taskNumber = readNumber(formerTaskFA);



        int maxRemain = 0;
        int maxRemainIndex = 0;
        for(int i = 1 ; i <= taskNumber && CPUNumber >= 0; i++){
            if(!isFinish(i) && useResource(i)){
               if(getResourceLength(i)*getRemainTime(i)>maxRemain){
                   maxRemain = getResourceLength(i)*getRemainTime(i);
                   maxRemainIndex = i;
                   cpuOperate[CPUNumber--] = maxRemainIndex;
                   timeleft(maxRemainIndex);
               }
            }
        }

    }


    /**
     * 记录剩余时间-1
     * @param taskID
     */
    private void timeleft(int taskID){
        int index = getTaskFA(taskID);
        int leftTime = readNumber(index+taskRemainTimeFA);
        if(leftTime == 0) return;
        leftTime--;
        writeNumber(index+taskRemainTimeFA, leftTime);
    }

    /**
     * 判断任务是否执行完毕
     * @param task
     * @return
     */
    private boolean isFinish(int task){
        int index = getTaskFA(task);
        int Time = readNumber(index+taskRemainTimeFA);
        return Time == 0;
    }


    private int getRemainTime(int task){
        int index = getTaskFA(task);
        int Time = readNumber(index+taskRemainTimeFA);
        return Time;
    }

    /**
     * 查看资源是否可用
     * @param taskID
     * @return
     */
    private boolean useResource(int taskID){
        int index = getTaskFA(taskID);
        int length = readNumber(index+taskResourceLengthFA);

        for(int i = 0 ; i < length ; i++){
            byte temp = readFreeMemory(index+taskResourceFA+i);
            if(readFreeMemory(resourceTableFA+temp-1) != 0) return false;
        }

        for(int i = 0 ; i < length ; i++){
            byte temp = readFreeMemory(index+taskResourceFA+i);
            writeFreeMemory(resourceTableFA+temp-1, (byte) 1);
        }
        return true;
    }




    /**
     * 初始化进程
     * @param task
     * @param arriveTime
     */
    private void initProcess(Task task, int arriveTime){
        int index =  getTaskIndex();
        writeNumber(index+taskIDFA, task.tid);
        writeNumber(index+taskArriveTimeFA, arriveTime);
        writeNumber(index+taskRequireTimeFA, task.cpuTime);
        writeNumber(index+taskRemainTimeFA, task.cpuTime);
        writeNumber(index+taskResourceLengthFA, task.resource.length);
        for(int i =0; i<task.resource.length;i++){
            writeFreeMemory(index + taskResourceFA+i, (byte) task.resource[i]);
        }
        writeNumber(formerTaskFA, task.tid);
        writeNumber(addressTableFA+task.tid*4, index);
    }

    /**
     * 准备内存空间
     */
    private void getReady(){
        for(int i=0; i<128; i++){
            writeFreeMemory(resourceTableFA+i, (byte)0);
        }
    }

    private int getTaskFA(int taskID){
        return readNumber(addressTableFA+taskID*4);
    }

    private int getResourceLength(int taskID){
        return readNumber(getTaskFA(taskID)+taskResourceLengthFA);
    }

    private int getTaskIndex(){
        int formerindex = readNumber(formerTaskFA);
        if(formerindex==0){
            return storageFA;
        }
        return getTaskFA(formerindex)+getResourceLength(formerindex)+taskResourceFA;
    }

    private int readNumber(int FA){
        int re = 0;
        re = re + (readFreeMemory(FA)&0xff)<<24;
        re = re + (readFreeMemory(FA+1)&0xff)<<16;
        re = re + (readFreeMemory(FA+2)&0xff)<<8;
        re = re + (readFreeMemory(FA+3)&0xff);
        return re;
    }

    private void writeNumber(int FA, int number){
        writeFreeMemory(FA+3, (byte) ((number&0x000000ff)));
        writeFreeMemory(FA+2, (byte) ((number&0x0000ff00)>>8));
        writeFreeMemory(FA+1, (byte) ((number&0x00ff0000)>>16));
        writeFreeMemory(FA, (byte) ((number&0xff000000)>>24));
    }


    /**
     * 执行主函数 用于debug
     * 里面的内容可随意修改
     * 你可以在这里进行对自己的策略进行测试，如果不喜欢这种测试方式，可以直接删除main函数
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // 定义cpu的数量
        int cpuNumber = 2;
        // 定义测试文件
        String filename = "src/testFile/test1.txt";

        BottomMonitor bottomMonitor = new BottomMonitor(filename,cpuNumber);
        BottomService bottomService = new BottomService(bottomMonitor);
        Schedule schedule =  new S161250059();
        schedule.setBottomService(bottomService);

        //外部调用实现类
        for(int i = 0 ; i < 450 ; i++){
            Task[] tasks = bottomMonitor.getTaskArrived();
            int[] cpuOperate = new int[cpuNumber];

            // 结果返回给cpuOperate
            schedule.ProcessSchedule(tasks,cpuOperate);

            try {
                bottomService.runCpu(cpuOperate);
            } catch (Exception e) {
                System.out.println("Fail: "+e.getMessage());
                e.printStackTrace();
                return;
            }
            bottomMonitor.increment();
        }

        //打印统计结果
        bottomMonitor.printStatistics();
        System.out.println();

        //打印任务队列
        bottomMonitor.printTaskArrayLog();
        System.out.println();

        //打印cpu日志
        bottomMonitor.printCpuLog();


        if(!bottomMonitor.isAllTaskFinish()){
            System.out.println(" Fail: At least one task has not been completed! ");
        }
    }

}
