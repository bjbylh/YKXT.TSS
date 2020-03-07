package srv.task;

import core.taskplan.InstructionManager;

/**
 * Created by lihan on 2020/3/7.
 */
public class InitHistoryInstruction {
    private static InitHistoryInstruction ourInstance = new InitHistoryInstruction();

    public static InitHistoryInstruction getInstance() {
        return ourInstance;
    }

    private InitHistoryInstruction() {

    }

    public void init(){
        InstructionManager instructionManager = new InstructionManager();
        instructionManager.init();
        instructionManager.close();
    }
}
