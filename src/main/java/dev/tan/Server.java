package dev.tan;

import io.javalin.Javalin;
import io.javalin.websocket.*;
import org.slf4j.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class Server {
    static class Message {
        private String user;
        private String text;

        public Message() {
        }

        public Message(String user, String text) {
            this.user = user;
            this.text = text;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
    private static Logger logger = LoggerFactory.getLogger(Server.class);
    private static ConcurrentHashMap<String, List<WsContext>> conMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7000);
        app.ws("/messages/:group", ws -> {
            ws.onConnect(ctx -> {
                String group = ctx.pathParam("group");
                System.out.println("Connecting to: " + group);
                List<WsContext> list = conMap.get(group);
                if(list != null) {
                    list.add(ctx);
                } else {
                    System.out.println("adding group: " + group);
                    list = Collections.synchronizedList(new LinkedList<WsContext>());
                    list.add(ctx);
                    conMap.put(group, list);
                }
                int members = list.size();
                Message m = new Message("System", "Connected to "+group+" ("+members+
                        (members == 1 ? " member)" : " members)"));
                ctx.send(m);
            });

            ws.onMessage(ctx -> {
                Message msg = ctx.message(Message.class);
                String group = ctx.pathParam("group");
                List<WsContext> list = conMap.get(group);

                list.forEach(c -> {
                    if(!c.equals(ctx))
                        c.send(msg);
                });
            });

            ws.onClose(ctx -> {
                String group = ctx.pathParam("group");
                System.out.println("Closing connection to " + group);
                List<WsContext> list = conMap.get(group);

                list.removeIf(c -> c.equals(ctx));
                int members = list.size();
                if(members == 0) {
                    System.out.println("removing group: " + group);
                    conMap.remove(group);
                    return;
                }
                list.forEach(c -> {
                    Message m = new Message("System", "A user has disconnected (" + members +
                            (members == 1 ? " member)" : " members)"));
                    c.send(m);
                });
            });
        });
    }
}
