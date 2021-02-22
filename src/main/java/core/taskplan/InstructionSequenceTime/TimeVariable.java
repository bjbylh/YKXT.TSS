package core.taskplan.InstructionSequenceTime;

public class TimeVariable {
    public double T0;        //成像开始时间       //2——该指令发出的时刻对应目标点的开始成像时刻
    public double T0d;       //成像结束时间       //2——该指令发出的时刻对应目标点的结束成像时刻
    public double T1;        //回放开始时间       //2——该指令发出的时刻对应地面站开始传输数据的时刻
    public double T1d;       //回放结束时间       //2——该指令发出的时刻对应地面站结束传输数据的时刻
    public double T;         //固存擦除任务擦除开始时间

    public double TSC;       //当前任务选择的数传开机指令序列的执行时长              TCAG01序列
                            //2——为当前任务选择的数传开机指令序列的执行时长，TCAG01序列、TCAG02序列、TCAG03序列、TCAG05序列，分别对应实传、记录、边记边放、实传加回放的模式选择
    public double TGF;       //当前任务选择的高分相机成像开机指令序列的执行时长       TCGFG01序列
                            //2——为当前任务选择的高分相机成像开机指令序列的执行时长，TCGFG01序列
    public double TGF2;      //当前任务选择的高分相机工作模式设置指令序列的执行时长   TCGFG02序列
    public double TDG1;      //当前任务选择的多光谱相机成像开机指令序列的执行时长     TCDGG01序列
                            //2——为当前任务的多光谱相机成像开机指令序列的执行时长，TCDGG01序列
    public double TDG2;      //当前任务选择的多光谱相机工作模式设置指令序列的执行时长  TCDGG02序列
                            //2——为多光谱相机谱段切换指令序列的执行时间,TCDGG02
    public double T4401;     //侧摆时长
    public double P24;       //凝视前机动时间
    public double P29;       //凝视指令提前发出时间
    public double TTCAG04;   //回放开机指令序列执行时间
}
