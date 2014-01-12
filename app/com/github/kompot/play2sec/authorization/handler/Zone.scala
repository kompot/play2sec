/*
 * Copyright 2012-2014 Joscha Feth, Steve Chaloner, Anton Fedchenko
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

package com.github.kompot.play2sec.authorization.handler

import com.github.kompot.play2sec.authorization.core.models.Subject

trait Zone {
  def allowedToEverybody = false
  def allowedToSelf = true
  def allowedToAdmin = true

  def name = getClass.getSimpleName

  def allowed(maybeSubject: Option[Subject], allowed: Option[Subject] => Boolean): Boolean
}
