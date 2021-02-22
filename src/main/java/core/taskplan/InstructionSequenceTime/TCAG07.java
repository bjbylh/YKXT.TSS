package core.taskplan.InstructionSequenceTime;

public class TCAG07 implements SequenceTime {

    @Override
    public String ExecutionTime(TimeVariable timeVariable, String TaskName) {
        if (TaskName == "TASK01") {
            double time1 = timeVariable.T0 - timeVariable.TGF;
            double time2 = timeVariable.T0 - timeVariable.TDG1;
            double time3 = timeVariable.T1;
            double time = (time1 < time2 ? time1 : time2) < time3 ? (time1 < time2 ? time1 : time2) : time3;//三个值取最小值
            time = time - 18;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "TASK10") {
            double time1 = timeVariable.T0 - 10;
            double time2 = timeVariable.T0 - timeVariable.TDG2-2;
            double time3 = timeVariable.T1;
            double time = (time1 < time2 ? time1 : time2) < time3 ? (time1 < time2 ? time1 : time2) : time3;//三个值取最小值
            time = time - 18;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }else if (TaskName == "TASK04") {
            double time = timeVariable.T1-5;//三个值取最小值
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        }



        if (TaskName == "TASK01" || TaskName == "TASK02") {
            double time1 = timeVariable.T0 - timeVariable.TGF;
            double time2 = timeVariable.T0 - timeVariable.TDG1;
            double time3 = timeVariable.T1;
            double time = (time1 < time2 ? time1 : time2) < time3 ? (time1 < time2 ? time1 : time2) : time3;//三个值取最小值
            time = time - 7;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        } else if (TaskName == "TASK10") {
            double time1 = timeVariable.T0 - 10;
            double time2 = timeVariable.T0 - timeVariable.TDG2-2;
            double time3 = timeVariable.T1;
            double time = (time1 < time2 ? time1 : time2) < time3 ? (time1 < time2 ? time1 : time2) : time3;//三个值取最小值
            time = time - 18;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        } else if (TaskName == "TASK04") {
            double time = timeVariable.T1 - 5;
            int i_time = (int) Math.ceil(time);

            return Integer.toHexString(i_time);
        } else {
            return null;
        }
    }
}
