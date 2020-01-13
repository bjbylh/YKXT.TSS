package core.taskplan.InstructionSequenceTime;

public class TCAG01 implements SequenceTime {

    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK01" || TaskName == "TASK02") {
            double time1=timeVariable.T0-timeVariable.TGF-timeVariable.TGF2;
            double time2=timeVariable.T0-timeVariable.TDG1-timeVariable.TDG2;
            double time3=timeVariable.T1;
            double time=(time1<time2?time1:time2)<time3?(time1<time2?time1:time2):time3;//三个值取最小值
            time=time-5-timeVariable.TSC;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(f_time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "TASK10") {
            double time1=timeVariable.T0-8;
            double time=time1<timeVariable.T1?time1:timeVariable.T1;
            time=time-5-timeVariable.TSC;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(f_time);

            return Integer.toHexString(i_time);
        }else {
            return null;
        }
    }
}
