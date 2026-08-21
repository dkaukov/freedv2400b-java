/*
 * This file is licensed under the GNU General Public License v3.0.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/gpl-3.0.html
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
/**
 * Pure-Java FreeDV 2400B discriminator-level modem.
 *
 * <p>Payloads contain 52 meaningful bits in seven MSB-first bytes. The low
 * nibble of byte six is padding. Decoder and streaming instances are stateful,
 * are not thread-safe, and must be dedicated to one sample stream.</p>
 */
package io.github.dkaukov.modem2400b;
