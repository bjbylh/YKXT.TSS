package core.taskplan.InstructionSequenceTime;

public class TCDGG02 implements SequenceTime {

    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK10") {
            double time = timeVariable.T0 - timeVariable.TDG2-2;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }
        //if (TaskName == "TASK01" || TaskName == "TASK02") {
        //    double time = timeVariable.T0 - timeVariable.TDG2;
        //    int i_time = (int) Math.ceil(time);
//
        //    return Integer.toHexString(i_time);
        //} else if (TaskName == "TASK10") {
        //    double time = timeVariable.T0 - timeVariable.TDG2 - 2;
        //    int i_time = (int) Math.ceil(time);
//
        //    return Integer.toHexString(i_time);
        //} else if (TaskName == "TASK06") {
        //    double time = timeVariable.T0 - timeVariable.TDG2;
        //    int i_time = (int) Math.ceil(time);
//
        //    return Integer.toHexString(i_time);
        //} else if (TaskName == "TASK08") {
        //    double time = timeVariable.T0d;
        //    int i_time = (int) Math.ceil(time);
//
        //    return Integer.toHexString(i_time);
        //}
        else {
            return null;
        }
    }
}
