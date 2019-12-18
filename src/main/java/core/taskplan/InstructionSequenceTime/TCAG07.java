package core.taskplan.InstructionSequenceTime;

public class TCAG07 implements SequenceTime {

    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK01") {
            double time1=timeVariable.T0-timeVariable.TGF;
            double time2=timeVariable.T0-timeVariable.TDG1-timeVariable.TDG2;
            double time=(time1<time2?time1:time2)<timeVariable.T1?(time1<time2?time1:time2):timeVariable.T1;//三个值取最小值
            time=time-5;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(f_time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "任务恢复") {
            double time1=timeVariable.T0-8;
            double time=time1<timeVariable.T1?time1:timeVariable.T1;
            time=time-5;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(f_time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "TASK04") {
            double time=timeVariable.T1-5;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(f_time);

            return Integer.toHexString(i_time);
        }else {
            return null;
        }
    }
}
