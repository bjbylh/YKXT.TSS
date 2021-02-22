package core.taskplan.InstructionSequenceTime;

public class TCKG02 implements SequenceTime {

    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK01") {
            double time = (timeVariable.T0d + 17) > timeVariable.T1d ? (timeVariable.T0d + 17) : timeVariable.T1d;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "TASK09") {
            double time = (timeVariable.T0d + 17) > timeVariable.T1d ? (timeVariable.T0d + 17) : timeVariable.T1d;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "TASK04") {
            double time = timeVariable.T1d;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "TASK05") {
            double time = timeVariable.T + 60;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }

        if (TaskName == "TASK01" || TaskName == "TASK03" || TaskName == "TASK09") {
            double time = (timeVariable.T0d + 17) > timeVariable.T1d ? (timeVariable.T0d + 17) : timeVariable.T1d;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }  else if (TaskName == "TASK04") {
            double time = timeVariable.T1d;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        } else if (TaskName == "TASK05") {
            double time = timeVariable.T + 60;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        } else {
            return null;
        }
    }
}
