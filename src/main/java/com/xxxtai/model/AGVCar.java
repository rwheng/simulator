package com.xxxtai.model;

import com.xxxtai.controller.AGVCpuRunnable;
import com.xxxtai.myToolKit.Orientation;
import com.xxxtai.myToolKit.State;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j(topic = "develop")
public class AGVCar {
    private int AGVNum;
    private Orientation orientation = Orientation.RIGHT;
    private ExecutorService executor;
    private Point position;
    private boolean finishEdge;
    private State state = State.STOP;
    private Edge atEdge;
    private boolean isFirstInquire = true;
    private int detectCardNum;
    private int lastDetectCardNum;
    private Map<Integer, Integer> cardCommandMap;
    private int stopCardNum;
    private long lastCommunicationTime;
    private long count_3s;
    @Resource
    private AGVCpuRunnable cpuRunnable;
    @Resource
    private Graph graph;

    public AGVCar() {
        this.executor = Executors.newSingleThreadExecutor();
        this.position = new Point(-100, -100);
        this.lastCommunicationTime = System.currentTimeMillis();
    }


    public void init(int AGVNum) {
        this.AGVNum = AGVNum;
        this.cardCommandMap = new HashMap<>();
        this.cpuRunnable.setCarModelToCpu(this);
        if (this.cpuRunnable.connect()) {
            this.executor.execute(this.cpuRunnable);
            this.cpuRunnable.heartBeat(AGVNum);
        }
        if (AGVNum == 1)
            setAtEdge(graph.getEdge(0));

        if (AGVNum == 2)
            setAtEdge(graph.getEdge(3));

        if (AGVNum == 3)
            setAtEdge(graph.getEdge(6));
    }

    private void setAtEdge(Edge edge) {
        this.atEdge = edge;
        this.position.x = this.atEdge.START_NODE.X;
        this.position.y = this.atEdge.START_NODE.Y;
        this.finishEdge = false;
        judgeOrientation();
    }

    public void stepByStep() {
        if (!finishEdge && (state == State.FORWARD || state == State.BACKWARD)
                && this.atEdge != null) {
            int FORWARD_PIX = 3;
            if (this.atEdge.START_NODE.X == this.atEdge.END_NODE.X) {
                if (this.atEdge.START_NODE.Y < this.atEdge.END_NODE.Y) {
                    if (this.position.y < this.atEdge.END_NODE.Y) {
                        this.position.y += FORWARD_PIX;
                    } else {
                        this.finishEdge = true;
                    }
                } else if (atEdge.START_NODE.Y > atEdge.END_NODE.Y) {
                    if (this.position.y > this.atEdge.END_NODE.Y) {
                        this.position.y -= FORWARD_PIX;
                    } else {
                        this.finishEdge = true;
                    }
                }
            } else if (this.atEdge.START_NODE.Y == this.atEdge.END_NODE.Y) {
                if (this.atEdge.START_NODE.X < this.atEdge.END_NODE.X) {
                    if (this.position.x < this.atEdge.END_NODE.X) {
                        this.position.x += FORWARD_PIX;
                    } else {
                        this.finishEdge = true;
                    }
                } else if (this.atEdge.START_NODE.X > this.atEdge.END_NODE.X) {
                    if (this.position.x > this.atEdge.END_NODE.X) {
                        this.position.x -= FORWARD_PIX;
                    } else {
                        this.finishEdge = true;
                    }
                }
            }
        }

        int cardNum = detectRFIDCard();
        if (cardNum != 0 && cardNum != this.detectCardNum) {
            this.lastDetectCardNum = this.detectCardNum;
            this.detectCardNum = cardNum;
            log.info(this.AGVNum + "AGV detectRFIDCard:" + cardNum);
            if (cardNum == this.stopCardNum) {
                this.state = State.STOP;
                this.cpuRunnable.sendStateToSystem(this.AGVNum, 2);
            }
            this.cpuRunnable.sendReadCardToSystem(this.AGVNum, cardNum);
        }

        if (this.finishEdge && this.isFirstInquire && this.cardCommandMap.get(this.lastDetectCardNum) != null) {
            if (!swerve(this.cardCommandMap.get(this.lastDetectCardNum))) {
                this.state = State.STOP;
            } else {
                this.cardCommandMap.remove(this.lastDetectCardNum);
            }
        }
    }

    public void heartBeat() {
        if (this.count_3s == 60) {
            this.count_3s = 0;
            this.cpuRunnable.heartBeat(this.AGVNum);
        } else {
            this.count_3s++;
        }
    }

    private void judgeOrientation() {
        if (atEdge.START_NODE.X == atEdge.END_NODE.X) {
            if (atEdge.START_NODE.Y < atEdge.END_NODE.Y) {
                orientation = Orientation.DOWN;
            } else {
                orientation = Orientation.UP;
            }
        } else if (atEdge.START_NODE.Y == atEdge.END_NODE.Y) {
            if (atEdge.START_NODE.X < atEdge.END_NODE.X) {
                orientation = Orientation.RIGHT;
            } else {
                orientation = Orientation.LEFT;
            }
        }
    }

    private boolean patrolLine(Orientation orientation) {
        boolean isFound = false;
        for (Edge e : graph.getEdgeArray()) {
            if (this.atEdge.END_NODE.CARD_NUM == e.START_NODE.CARD_NUM && this.atEdge.START_NODE.CARD_NUM != e.END_NODE.CARD_NUM) {
                if ((orientation == Orientation.RIGHT && e.START_NODE.Y == e.END_NODE.Y && e.START_NODE.X < e.END_NODE.X)
                        || (orientation == Orientation.DOWN && e.START_NODE.X == e.END_NODE.X && e.START_NODE.Y < e.END_NODE.Y)
                        || (orientation == Orientation.LEFT && e.START_NODE.Y == e.END_NODE.Y && e.START_NODE.X > e.END_NODE.X)
                        || (orientation == Orientation.UP && e.START_NODE.X == e.END_NODE.X && e.START_NODE.Y > e.END_NODE.Y)) {
                    setAtEdge(e);
                    isFound = true;
                    break;
                }
            } else if (this.atEdge.END_NODE.CARD_NUM == e.END_NODE.CARD_NUM && this.atEdge.START_NODE.CARD_NUM != e.START_NODE.CARD_NUM) {
                if ((orientation == Orientation.RIGHT && e.START_NODE.Y == e.END_NODE.Y && e.START_NODE.X > e.END_NODE.X)
                        || (orientation == Orientation.DOWN && e.START_NODE.X == e.END_NODE.X && e.START_NODE.Y > e.END_NODE.Y)
                        || (orientation == Orientation.LEFT && e.START_NODE.Y == e.END_NODE.Y && e.START_NODE.X < e.END_NODE.X)
                        || (orientation == Orientation.UP && e.START_NODE.X == e.END_NODE.X && e.START_NODE.Y < e.END_NODE.Y)) {
                    setAtEdge(new Edge(e.END_NODE, e.START_NODE, e.REAL_DISTANCE, e.CARD_NUM));
                    isFound = true;
                    break;
                }
            }
        }
        return isFound;
    }

    private boolean swerve(int signal) {//1、左转；2、右转；3、前进
        boolean isFound = false;
        this.isFirstInquire = false;
        if (signal == 1) {
            switch (this.orientation) {
                case RIGHT:
                    isFound = patrolLine(Orientation.UP);
                    break;
                case LEFT:
                    isFound = patrolLine(Orientation.DOWN);
                    break;
                case UP:
                    isFound = patrolLine(Orientation.LEFT);
                    break;
                case DOWN:
                    isFound = patrolLine(Orientation.RIGHT);
                    break;
            }
        } else if (signal == 2) {
            switch (this.orientation) {
                case RIGHT:
                    isFound = patrolLine(Orientation.DOWN);
                    break;
                case LEFT:
                    isFound = patrolLine(Orientation.UP);
                    break;
                case UP:
                    isFound = patrolLine(Orientation.RIGHT);
                    break;
                case DOWN:
                    isFound = patrolLine(Orientation.LEFT);
                    break;
            }
        } else if (signal == 3) {
            isFound = patrolLine(this.orientation);
        }
        if (isFound) {
            this.isFirstInquire = true;
            this.state = State.FORWARD;
        }
        return isFound;
    }

    private int detectRFIDCard() {
        int foundCard = 0;
        if (Math.abs(this.position.x - this.atEdge.CARD_POSITION.x) < 4 && Math.abs(this.position.y - this.atEdge.CARD_POSITION.y) < 4)
            foundCard = this.atEdge.CARD_NUM;

        if (Math.abs(this.position.x - this.atEdge.START_NODE.X) < 4 && Math.abs(this.position.y - this.atEdge.START_NODE.Y) < 4)
            foundCard = this.atEdge.START_NODE.CARD_NUM;

        if (Math.abs(this.position.x - this.atEdge.END_NODE.X) < 4 && Math.abs(this.position.y - this.atEdge.END_NODE.Y) < 4)
            foundCard = this.atEdge.END_NODE.CARD_NUM;

        return foundCard;
    }

    public void setCardCommandMap(String commandString) {
        String[] commandArray = commandString.split("/");
        stopCardNum = Integer.parseInt(commandArray[commandArray.length - 1], 16);
        for (int i = 0; i < commandArray.length - 1; i++) {
            log.info("commandString:{}", commandArray[i]);
            String[] c = commandArray[i].split(",");
            this.cardCommandMap.put(Integer.parseInt(c[0],16), Integer.parseInt(c[1],16));
        }
        this.state = State.FORWARD;
    }

    public void changeState() {
        if (this.state == State.FORWARD || this.state == State.BACKWARD) {
            this.cpuRunnable.sendStateToSystem(AGVNum, 2);
            this.state = State.STOP;
        } else if (this.state == State.STOP) {
            this.state = State.FORWARD;
            this.cpuRunnable.sendStateToSystem(AGVNum, 1);
        }
    }

    public void stopTheAGV() {
        this.state = State.STOP;
        this.cpuRunnable.sendStateToSystem(AGVNum, 2);
    }

    public void startTheAGV() {
        this.state = State.FORWARD;
        this.cpuRunnable.sendStateToSystem(AGVNum, 1);
    }

    public int getX() {
        return this.position.x;
    }

    public int getY() {
        return this.position.y;
    }

    public Orientation getOrientation() {
        return this.orientation;
    }

    public long getLastCommunicationTime() {
        return this.lastCommunicationTime;
    }

    public void setLastCommunicationTime(long time) {
        this.lastCommunicationTime = time;
    }

    public void setNewCpuRunnable() {
        this.cpuRunnable = new AGVCpuRunnable();
        this.cpuRunnable.setCarModelToCpu(this);
        if (this.cpuRunnable.connect()) {
            this.executor.execute(this.cpuRunnable);
            this.cpuRunnable.sendStateToSystem(AGVNum, 2);
        }
    }
}
