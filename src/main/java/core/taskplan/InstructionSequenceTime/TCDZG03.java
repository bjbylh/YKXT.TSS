package core.taskplan.InstructionSequenceTime;

public class TCDZG03 implements SequenceTime {

    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK10") {

            double time1 = timeVariable.T0 - 10;
            double time2 = timeVariable.T0 - timeVariable.TDG2-2;
            double time3 = timeVariable.T1;
            double time = (time1 < time2 ? time1 : time2) < time3 ? (time1 < time2 ? time1 : time2) : time3;//三个值取最小值
            time = time +8;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else {
            return null;
        }
    }
}
