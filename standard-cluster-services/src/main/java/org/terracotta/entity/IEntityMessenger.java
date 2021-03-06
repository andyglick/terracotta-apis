/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.entity;

import com.tc.classloader.CommonComponent;


/**
 * The common interface for a built-in service which provides the ability for an entity to asynchronously message itself.
 * This is also the underlying mechanism which allows entities to contact each other (as they can safely expose code which
 * doesn't touch their state, but calls through to this interface, thus allowing a future invocation).
 * This is because an entity can't safely call into another one since its execution state is undefined, relative to another
 * entity, and any action taken by such a cross-entity call would not be replicated to passive entities.
 */
@CommonComponent
public interface IEntityMessenger {
  /**
   * Asynchronously send a message to the entity instance which looked up the service instance.
   * 
   * @param message The message to send.
   * @throws MessageCodecException The message could not be serialized.
   */
  void messageSelf(EntityMessage message) throws MessageCodecException;

  /**
   * Defer a message based on another message, which will be sent in the future.
   *
   * @param tag String reason tag, mainly for debugging.
   * @param originalMessageToDefer original message to defer
   * @param futureMessage message to use to complete original message
   * @return handle used to release the deferred message
   */
  ExplicitRetirementHandle deferRetirement(String tag,
                                           EntityMessage originalMessageToDefer,
                                           EntityMessage futureMessage);

  /**
   * Asynchronously send a message to the entity instance which looked up the service instance but also blocks the final
   * retirement acknowledgement of originalMessageToDefer until newMessageToSchedule completes.
   * 
   * @param originalMessageToDefer The currently executing message whose retirement must be deferred.
   * @param newMessageToSchedule The new message to send.
   * @throws MessageCodecException The message could not be serialized.
   */
  void messageSelfAndDeferRetirement(EntityMessage originalMessageToDefer, EntityMessage newMessageToSchedule) throws MessageCodecException;

  /**
   * Requests that the given message be send to the entity instance after millisBeforeSend have elapsed.
   * The message will be sent once, and may arrive late but won't arrive before millisBeforeSend have elapsed.
   * The message will not arrive if the server restarts or the entity is destroyed before it was due.
   * 
   * @param message The new message to send.
   * @param millisBeforeSend The minimum number of milliseconds which must pass before message should be delivered.
   * @return A token to describe this delayed send which can be used to cancel it.
   * @throws MessageCodecException The message could not be serialized.
   */
  ScheduledToken messageSelfAfterDelay(EntityMessage message, long millisBeforeSend) throws MessageCodecException;

  /**
   * Requests that the given message be send to the entity instance every millisBetweenSends milliseconds.
   * The message will be sent continuously with the first arriving after millisBetweenSends and then again after each
   *  millisBetweenSends milliseconds.  Note that drift in the actual delivery time will not change the scheduled time of
   *  each following message delivery at N*millisBetweenSends milliseconds from when this call was made.
   * Note that any message send may arrive late but won't arrive before N*millisBetweenSends have elapsed.
   * This message will continue to be re-sent until it is canceled, the server restarts, or the entity is destroyed.
   * 
   * @param message The new message to send.
   * @param millisBetweenSends The minimum number of milliseconds which must pass before each message should be delivered.
   * @return A token to describe this delayed send which can be used to cancel it.
   * @throws MessageCodecException The message could not be serialized.
   */
  ScheduledToken messageSelfPeriodically(EntityMessage message, long millisBetweenSends) throws MessageCodecException;

  /**
   * Called to cancel a previously scheduled delayed or periodic message.
   * 
   * @param token The token previously received for a scheduled message.
   */
  void cancelTimedMessage(ScheduledToken token);


  /**
   * An opaque interface used as the token type for canceling previously scheduled messages.
   */
  public interface ScheduledToken {
  }
}
