/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.webcam;

import bisq.common.webcam.ControlSignals;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import static bisq.common.encoding.NonPrintingCharacters.UNIT_SEPARATOR;
import static bisq.common.webcam.ControlSignals.*;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class InputHandler {
    private final WebcamAppModel model;

    InputHandler(WebcamAppModel model) {
        this.model = model;
    }

    public void onSocket(Socket socket) {
        try (Scanner scanner = new Scanner(socket.getInputStream())) {
            StringBuilder stringBuilder = new StringBuilder();
            // We only expect one line
            while (scanner.hasNextLine() && stringBuilder.length() == 0) {
                String line = scanner.nextLine();
                stringBuilder.append(line);
            }

            String message = stringBuilder.toString();
            // LN invoice is usually about 230 chars. We tolerate 1000 in message validation
            checkArgument(message.length() < 1000, "Received message exceeds out limit of 1000 chars");
            processMessage(message);
        } catch (IOException e) {
            model.getLocalException().set(e);
        }
    }

    private void processMessage(String message) {
        String separator = String.valueOf(UNIT_SEPARATOR.getNonPrintingChar());
        if (!message.startsWith(separator)) {
            throw new IllegalArgumentException("Message does not start with expected token. Message=" + message);
        }
        message = message.replaceFirst(separator, "");
        if (message.contains(separator)) {
            String[] tokens = message.split(separator);
            String signal = tokens[0];
            String payload = tokens[1];
            if (signal.equals(QR_CODE_PREFIX.name())) {
                model.getQrCode().set(payload);
            } else if (signal.equals(ERROR_MESSAGE_PREFIX.name())) {
                model.getWebcamAppErrorMessage().set(payload);
            } else {
                throw new IllegalArgumentException("Not recognized message type. Message=" + message);
            }
        } else {
            if (ControlSignals.SHUTDOWN.name().equals(message)) {
                model.getIsShutdownSignalReceived().set(true);
            } else if (IMAGE_RECOGNIZED.name().equals(message)) {
                model.getImageRecognized().set(true);
            } else if (HEART_BEAT.name().equals(message)) {
                log.debug(message);
                model.getLastHeartBeatTimestamp().set(System.currentTimeMillis());
            } else {
                throw new IllegalArgumentException("Not recognized message type. Message=" + message);
            }
        }


        /*
        *   qrCodeListeningServer.getQrCode().addObserver(qrCode -> {
            if (qrCode != null) {
                log.info("Qr code detected. We close webcam app and stop our qrCodeListener server.");
                shutDownWebcam();
                UIThread.run(() -> {
                    model.bitcoinPaymentData.set(qrCode);
                    model.getIsWebcamAppState().set(QR_CODE_DETECTED);
                });
            }
        });
        qrCodeListeningServer.getImageRecognized().addObserver(value -> {
            if (value) {
                model.getIsWebcamAppState().set(IMAGE_RECOGNIZED);
            }
        });
        qrCodeListeningServer.getException().addObserver(exception -> {
            if (exception != null) {
                shutDownWebcam();
                handleWebcamException(exception);
            }
        });
        qrCodeListeningServer.getWebcamAppErrorMessage().addObserver(errorMessage -> {
            if (errorMessage != null) {
                shutDownWebcam();
                UIThread.run(() -> {
                    model.getWebcamErrorMessage().set(errorMessage);
                });
            }
        });

        qrCodeListeningServer.getIsShutdown().addObserver(isShutdown -> {
            if (isShutdown) {
                log.info("Webcam app got closed without detecting a qr code. We stop our qrCodeListener server.");
                qrCodeListeningServer.stopServer();
                UIThread.run(() -> model.getIsWebcamAppState().set(STOPPED));
            }
        });
        * */
    }
}
