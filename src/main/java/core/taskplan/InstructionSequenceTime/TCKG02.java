package core.taskplan.InstructionSequenceTime;

public class TCKG02 implements SequenceTime {

    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK01" || TaskName == "TASK03") {
            double time=timeVariable.T0d>timeVariable.T1d?timeVariable.T0d:timeVariable.T1d;
//            float f_time=(float) time;
            int i_time= (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "TASK09") {
            double time=timeVariable.T0d>timeVariable.T1d?timeVariable.T0d:timeVariable.T1d;
//            float f_time=(float) time;
            int i_time= (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "TASK04") {
            double time=timeVariable.T1d;
//            float f_time=(float) time;
            int i_time= (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "TASK05") {
            double time=timeVariable.T+60;
//            float f_time=(float) time;
            int i_time= (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else {
            return null;
        }
    }
}
