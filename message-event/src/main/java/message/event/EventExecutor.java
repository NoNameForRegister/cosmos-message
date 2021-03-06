package message.event;

import message.event.module.BaseEvent;
import message.event.listener.AbstractEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 事件执行器.
 *
 * @author sunhao(sunhao.java@gmail.com)
 * @version V1.0
 * @createTime 13-4-27 上午4:57
 */
@Component
public class EventExecutor {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(EventExecutor.class);

    /**
     * 同步事件Map.
     */
    private final Map<Class<? extends BaseEvent>, List<AbstractEventListener>> syncEventListeners = new HashMap<Class<? extends BaseEvent>, List<AbstractEventListener>>();

    /**
     * 异步事件Map.
     */
    private final Map<Class<? extends BaseEvent>, List<AbstractEventListener>>  asyncEventListeners = new HashMap<Class<? extends BaseEvent>, List<AbstractEventListener>>();

    private final Object REGISTER_LOCK_OBJECT = new Object();

    /**
     * 注册监听服务
     *
     * @param eventTypes        事件类型
     * @param listener          监听器
     * @param isSync            是否是同步执行
     */
    public void registerListener(List<Class<? extends BaseEvent>> eventTypes, AbstractEventListener listener, boolean isSync){
        synchronized (REGISTER_LOCK_OBJECT) {
            if(eventTypes == null || eventTypes.isEmpty()){
                logger.debug("module types is null!");
                return;
            }

            logger.debug("regist listener '{}' for module type '{}'!", listener, eventTypes);

            Map<Class<? extends BaseEvent>, List<AbstractEventListener>> eventListeners = isSync ? syncEventListeners : asyncEventListeners;
            for(Iterator<Class<? extends BaseEvent>> it = eventTypes.iterator(); it.hasNext(); ){
                Class<? extends BaseEvent> eventType = it.next();
                if(eventType == null)
                    continue;

                List<AbstractEventListener> listeners = eventListeners.get(eventType);
                if(listeners == null){
                    listeners = new ArrayList<AbstractEventListener>();
                }

                listeners.add(listener);
                eventListeners.put(eventType, listeners);
            }
        }
    }

    /**
     * 执行事件
     *
     * @param event
     */
    public void executeEvent(BaseEvent event) throws BaseEventException {
        //1.先执行同步事件
        List<AbstractEventListener> syncListeners = syncEventListeners.get(event.getClass());
        if(syncListeners != null && !syncListeners.isEmpty()){
            for(Iterator<AbstractEventListener> it = syncListeners.iterator(); it.hasNext(); ){
                AbstractEventListener listener = it.next();

                logger.debug("execute module '{}' use listener '{}'!", event, listener);
                //执行
                try {
                    listener.handleEvent(event);
                } catch (BaseEventException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        //2.执行异步事件
        List<AbstractEventListener> asyncListeners = asyncEventListeners.get(event.getClass());
        if(asyncListeners != null && !asyncListeners.isEmpty()){
            executeAsyncEvent(asyncListeners, event);
        }
    }

    /**
     * 执行异步事件
     *
     * @param asyncListeners        异步事件监听
     * @param event                 异步事件
     */
    private void executeAsyncEvent(final List<AbstractEventListener> asyncListeners, final BaseEvent event) throws BaseEventException {
        new Thread(){
            public void run() {
                for(Iterator<AbstractEventListener> it = asyncListeners.iterator(); it.hasNext(); ){
                    AbstractEventListener listener = it.next();

                    logger.debug("execute module '{}' use listener '{}'!", event, listener);
                    //执行
                    try {
                        listener.handleEvent(event);
                    } catch (BaseEventException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }.start();
    }
}
