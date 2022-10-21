package Manager;

import sun.misc.Signal;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Manager {

    public static int xValue;
    private static List<Runnable> functionsProcessTasks = null;
    public static ProcessBuilder processBuilderF;
    public static ProcessBuilder processBuilderG;
    private static Process fProcess;
    private static Process gProcess;
    private static Executor executor;
    private static Integer countOfOperationToDo;
    private static boolean cancel;
    private static ServerSocket server;
    private static  List<Integer> ProcessesResults = new ArrayList<>();
    private static Boolean fProcessFaild;
    private static Boolean gProcessFailed;
    private static  int softFailCounterF;
    private static  int softFailCounterG;

    public static void main(String[] arg) throws IOException {
        startCheckTheCancel();
        xValue = 76876;
        fProcessFaild = false;
        gProcessFailed = false;
        functionsProcessTasks = new ArrayList<>(
                List.of(() -> {
                            try {
                                fProcess = processBuilderF.start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        },
                        () -> {
                            try {
                                gProcess =  processBuilderG.start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                ));

        countOfOperationToDo = 2;
        cancel = false;
        server = new ServerSocket(1009);

        executor = Executors.newFixedThreadPool(2);

        String classPath = Objects.requireNonNull(Manager.class.getClassLoader().getResource(".")).toString();
        processBuilderF = new ProcessBuilder("java", "-cp", classPath, "processes.processF");
        processBuilderG = new ProcessBuilder("java", "-cp", classPath, "processes.processG");
        for(Runnable task: functionsProcessTasks) {
            executor.execute(task);
        }
        sendXValueToFunctionProcesses();
        while (true){
            if(!fProcess.isAlive() && !gProcess.isAlive()){
                readResults();
                if(countOfOperationToDo == 0) {
                    break;
                } else if(cancel) {
                    break;
                } else {
                    sendXValueToFunctionProcesses();
                }
            } else if(cancel) {
                break;
            }
        }
        Integer fStatus = fProcessFaild ? -1 : fProcess.isAlive() ? softFailCounterF + 1 : 0;
        Integer gStatus = gProcessFailed ? -1 : gProcess.isAlive() ? softFailCounterG + 1 : 0;

        if(fStatus + gStatus == -2) {
            System.out.println("Expression value: failed");
        } else if(fStatus == 0 && gStatus == 0) {
            System.out.println("Expression value: " + getResultAfterProcesses(ProcessesResults.get(0),ProcessesResults.get(1)));
        } else {
            System.out.println("Expression value: underfined");
        }
        System.exit(0);
    }

    private static Integer getResultAfterProcesses(int x, int y){
        return x * y;
    }

    private static void sendXValueToFunctionProcesses(){
        for(int i = 0; i < countOfOperationToDo; i++) {
            try (
                    Socket socket = server.accept();
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
            ){
                out.writeObject(xValue);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void startCheckTheCancel(){
        Signal.handle(new Signal("INT"), signal -> cancelMenuIfSignal());
    }
    private static void cancelMenuIfSignal() {
        System.out.println("Please confirm that computation should be stopped by y or n");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        long referenceTime = new Date().getTime();
        while ((System.currentTimeMillis()-referenceTime)<5000) {
            try {
                if (reader.ready()) {
                    String s = "";
                    try {
                        s = reader.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (s.equals("y")) {
                        cancel = true;
                        return;
                    } else if (s.equals("n")) {
                        startCheckTheCancel();
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        System.out.println("Time, continue");
    }

    private static void readResults(){
        int tmpCountOfOperations = countOfOperationToDo;
        for(int i = 0; i < countOfOperationToDo; i++) {
            try (Socket socket = server.accept()) {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                if(cancel){
                    return;
                }
                String res = (String) in.readObject();
                String processIndex = res.substring(0, 1);
                res = res.substring(1);

                if(res.equals("hard")){
                    if(processIndex.equals("f")) {
                        fProcessFaild = true;
                    } else {
                        gProcessFailed = true;
                    }
                    System.out.println(processIndex + " hard failed");
                    tmpCountOfOperations--;
                } else if (res.equals("soft")){
                    if(processIndex.equals("f")) {
                        if(softFailCounterF < 5) {
                            executor.execute(functionsProcessTasks.get(0));
                            softFailCounterF++;
                            System.out.println(processIndex + " soft failed");
                        } else {
                            fProcessFaild = true;
                            System.out.println(processIndex + " hard failed");
                            tmpCountOfOperations--;
                        }
                    } else {
                        if(softFailCounterG < 5) {
                            executor.execute(functionsProcessTasks.get(1));
                            softFailCounterG++;
                            System.out.println(processIndex + " soft failed");
                        } else {
                            gProcessFailed = true;
                            System.out.println(processIndex + " hard failed");
                            tmpCountOfOperations--;
                        }
                    }
                } else {
                    ProcessesResults.add(Integer.parseInt(res));
                    tmpCountOfOperations--;
                }
            } catch (IOException | ClassNotFoundException e ) {
                System.out.println(e);
            }
        }
        countOfOperationToDo = tmpCountOfOperations;
    }
}

