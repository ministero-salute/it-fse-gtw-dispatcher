package it.finanze.sanita.fse2.ms.gtw.dispatcher.logging;

/**
 * @since 0.0.1
 */
public interface FailedDeliveryCallback<E> {
	
    void onFailedDelivery(E evt, Throwable throwable);
    
}