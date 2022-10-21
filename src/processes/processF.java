package processes;

import sun.misc.Signal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.function.Function;

public class processF {
    private static Function<Integer, Optional<Optional<Integer>>> function;
    private  static Integer value;
    private static String res;

    public static void main(String[] args) {
        startCheckTheCancel();
        function = IntOps::trialF;
        try (
                Socket socket = new Socket("localhost", 1009);
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))
        {
            value = (Integer) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }

        Optional<Optional<Integer>> packed_result = function.apply(value);

        if(!packed_result.isPresent()){
            res =  "fhard";
        } else if (!packed_result.get().isPresent()){
            res =  "fsoft";
        } else {
            res = String.valueOf(packed_result.get().get());
        }
        try (
                Socket socket = new Socket("localhost", 1009);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()))
        {
            out.writeObject(res);
            out.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void startCheckTheCancel(){
        Signal.handle(new Signal("INT"), signal -> startCheckTheCancel());
    }
}
