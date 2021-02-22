package core.taskplan.InstructionSequenceTime;

public class TCA305 implements SequenceTime {

    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK01") {
            double time=timeVariable.T0d;
            //if (time < timeVariable.T1d) {
            //    time=timeVariable.T1d;
            //}
            float f_time=(float) time;
            int i_time= (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "TASK09") {
            double time=timeVariable.T0d;
            //if (time < timeVariable.T1d) {
            //    time=timeVariable.T1d;
            //}
            float f_time=(float) time;
            int i_time= (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }

        if (TaskName == "TASK01") {
            double time=timeVariable.T0d;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "任务暂停") {
            double time=timeVariable.T0d;
            float f_time=(float) time;
            int i_time= (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else {
            return null;
        }
    }
}
