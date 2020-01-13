package core.taskplan.InstructionSequenceTime;

public class TCKG01 implements SequenceTime {

    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK01" || TaskName == "TASK02") {
            double time1=timeVariable.T0-timeVariable.TGF-timeVariable.TGF2;
            double time2=timeVariable.T0-timeVariable.TDG1-timeVariable.TDG2;
            double time3=timeVariable.T1;
            double time=(time1<time2?time1:time2)<time3?(time1<time2?time1:time2):time3;//三个值取最小值
            time=time-6-timeVariable.TSC;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(f_time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "任务恢复") {
            double time1=timeVariable.T0-8;
            double time=time1<timeVariable.T1?time1:timeVariable.T1;
            time=time-6-timeVariable.TSC;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(f_time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "TASK04") {
            double time=timeVariable.T1-timeVariable.TTCAG04-5;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(f_time);

            return Integer.toHexString(i_time);
        }else {
            return null;
        }
    }
}
