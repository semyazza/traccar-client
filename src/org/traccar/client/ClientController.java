/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.client;

import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.os.Handler;


public class ClientController implements Connection.ConnectionHandler {

    public static final long RECONNECT_DELAY = 10 * 1000;

    private Context context;

    private Handler handler;
    private Queue<String> messageQueue;

    private Connection connection;

    private String address;
    private int port;
    private String loginMessage;

    public ClientController(Context context, String address, int port, String loginMessage) {
        this.context = context;
        messageQueue = new LinkedList<String>();
        this.address = address;
        this.port = port;
        this.loginMessage = loginMessage;
    }

    public void start() {
        handler = new Handler();
        connection = new Connection(this);
        connection.connect(address, port);
    }

    public void stop() {
        connection.close();
        handler.removeCallbacksAndMessages(null);
    }

    private void reconnect() {
        handler.removeCallbacksAndMessages(null);
        connection.close();
        connection = new Connection(this);
        connection.connect(address, port);
    }

    private void delayedReconnect() {
        connection.close();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                connection = new Connection(ClientController.this);
                connection.connect(address, port);
            }
        }, RECONNECT_DELAY);
    }

    public void setNewServer(String address, int port) {
        this.address = address;
        this.port = port;
        reconnect();
    }

    public void setNewLogin(String loginMessage) {
        this.loginMessage = loginMessage;
        reconnect();
    }

    public void setNewLocation(String locationMessage) {
        messageQueue.offer(locationMessage);
        if (!connection.isClosed() && !connection.isBusy()) {
            connection.send(messageQueue.poll());
        }
    }

    @Override
    public void onConnected(boolean result) {
        if (result) {
            StatusActivity.addMessage(context.getString((R.string.status_connection_success)));
            connection.send(loginMessage);
        } else {
            StatusActivity.addMessage(context.getString((R.string.status_connection_fail)));
            delayedReconnect();
        }
    }

    @Override
    public void onSent(boolean result) {
        if (result) {
            if (!messageQueue.isEmpty()) {
                connection.send(messageQueue.poll());
            }
        } else {
            StatusActivity.addMessage(context.getString((R.string.status_send_fail)));
            delayedReconnect();
        }
    }

}
