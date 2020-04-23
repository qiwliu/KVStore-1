package kvstore.consistency;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import kvstore.servers.AckReq;
import kvstore.servers.AckResp;
import kvstore.servers.Worker;
import kvstore.servers.WorkerServiceGrpc;

public class BcastAckTask extends taskEntry {
    private static final Logger logger = Logger.getLogger(BcastAckTask.class.getName());
    private List<Worker.ServerConfiguration> workerConf;
    private int senderId;

    /**
     * @param localClock The clock of the message to acknowledge
     * @param id         The id of the message to acknowledge
     * @param acksNum    the number acknowledgement required to delivery the message
     */
    public BcastAckTask(AtomicInteger globalClock, int localClock, int id, int senderId,
            List<Worker.ServerConfiguration> workerConf) {
        super(globalClock, localClock, id);
        this.workerConf = workerConf;
        this.senderId = senderId;
    }

    /**
     * Acknowledgement back to other workers for the specified message
     */
    @Override
    public void run() {
        globalClock.incrementAndGet(); /* Update the clock for sending acks */
        /* Send acks including the self */
        for (int i = 0; i < workerConf.size(); i++) {
            Worker.ServerConfiguration sc = this.workerConf.get(i);
            ManagedChannel channel = ManagedChannelBuilder.forAddress(sc.ip, sc.port).usePlaintext().build();
            WorkerServiceGrpc.WorkerServiceBlockingStub stub = WorkerServiceGrpc.newBlockingStub(channel);

            AckReq request = AckReq.newBuilder().setClock(localClock).setId(id).setReceiver(i).setSender(senderId)
                    .build();
            AckResp resp = stub.handleAck(request);

            // logger.info(String.format("Worker[%d] --ACK_Message[%d][%d]--> Worker[%d]", senderId, request.getClock(),
            //         request.getId(), i));
            channel.shutdown();
        }
    }

}