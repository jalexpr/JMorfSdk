/*
 * Copyright (C) 2017  Alexander Porechny alex.porechny@mail.ru
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Attribution-NonCommercial-ShareAlike 3.0 Unported
 * (CC BY-SA 3.0) as published by the Creative Commons.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Attribution-NonCommercial-ShareAlike 3.0 Unported (CC BY-SA 3.0)
 * for more details.
 *
 * You should have received a copy of the Attribution-NonCommercial-ShareAlike
 * 3.0 Unported (CC BY-SA 3.0) along with this program.
 * If not, see <https://creativecommons.org/licenses/by-nc-sa/3.0/legalcode>
 *
 *
 * Copyright (C) 2017 Александр Поречный alex.porechny@mail.ru
 *
 * Эта программа свободного ПО: Вы можете распространять и / или изменять ее
 * в соответствии с условиями Attribution-NonCommercial-ShareAlike 3.0 Unported
 * (CC BY-SA 3.0), опубликованными Creative Commons.
 *
 * Эта программа распространяется в надежде, что она будет полезна,
 * но БЕЗ КАКИХ-ЛИБО ГАРАНТИЙ; без подразумеваемой гарантии
 * КОММЕРЧЕСКАЯ ПРИГОДНОСТЬ ИЛИ ПРИГОДНОСТЬ ДЛЯ ОПРЕДЕЛЕННОЙ ЦЕЛИ.
 * См. Attribution-NonCommercial-ShareAlike 3.0 Unported (CC BY-SA 3.0)
 * для более подробной информации.
 *
 * Вы должны были получить копию Attribution-NonCommercial-ShareAlike 3.0
 * Unported (CC BY-SA 3.0) вместе с этой программой.
 * Если нет, см. <Https://creativecommons.org/licenses/by-nc-sa/3.0/legalcode>
 */
package jmorfsdk.load;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jmorfsdk.JMorfSdk;
import jmorfsdk.form.InitialForm;
import jmorfsdk.form.WordForm;

public final class LoadBasedOnHashCodeFromZip implements LoadFromFile {

    private int pointer = 0;
    private int sizeFileHashAndMorfCharacteristics = 0;
    private static final int CONTROLVALUE = -1;//new byte[]{0, 0, 0, 0, 0, 0, 0, 0};

    @Override
    public JMorfSdk loadFullLibrary() {
        JMorfSdk jMorfSdk = loadLibraryForSearchInitialForm();
        loadWordFormsString(jMorfSdk);
        return jMorfSdk;

    }

    @Override
    public JMorfSdk loadLibraryForSearchInitialForm() {
        JMorfSdk jMorfSdk = new JMorfSdk();
        loadLibraryToFindInitialFormAndMorfCharacteristecZip(jMorfSdk);
        return jMorfSdk;
    }

    public void loadLibraryToFindInitialFormAndMorfCharacteristecZip(JMorfSdk jMorfSdk) {
        ZipInputStream streamHashCodeAndMorfCharacteristic = null;
        Scanner scannerInitialFormString = null;
        try {
            streamHashCodeAndMorfCharacteristic = openZipFile(Property.pathHashAndMorfCharacteristics);
            scannerInitialFormString = openScannerFromZipFile(Property.pathInitialFormString, Property.encoding);
            loadHashAndString(jMorfSdk, streamHashCodeAndMorfCharacteristic, scannerInitialFormString);
        } catch (Exception ex) {
            Logger.getLogger(LoadBasedOnHashCodeFromZip.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeStream(streamHashCodeAndMorfCharacteristic);
            closeScanner(scannerInitialFormString);
        }
    }

    private ZipInputStream openZipFile(String nameLibrary) throws Exception {
        return getInputStreamZipFile(new File(Property.pathZipDictionary), nameLibrary);
    }

    private ZipInputStream getInputStreamZipFile(File zip, String nameLibrary) throws IOException {
        ZipInputStream zin = new ZipInputStream(new FileInputStream(zip));
        for (ZipEntry e; (e = zin.getNextEntry()) != null;) {
            if (e.getName().equals(nameLibrary)) {
                if(sizeFileHashAndMorfCharacteristics == 0) {
                sizeFileHashAndMorfCharacteristics = (int) e.getSize();
                }
                return zin;
            }
        }
        throw new EOFException("Cannot find " + nameLibrary);
    }

    private Scanner openScannerFromZipFile(String pathLibrary, String encoding) throws Exception {
        try {
            return new Scanner(getInputStreamZipFile(new File(Property.pathZipDictionary), pathLibrary), encoding);
        } catch (FileNotFoundException ex) {
            String messages = String.format("Ошибка при чтении файла.\r\nПроверте наличие %s, в случае отсуствия скачайте с репозитория %s\r\n", pathLibrary, MYREPOSITORY);
            Logger.getLogger(LoadBasedOnHashCodeFromZip.class.getName()).log(Level.SEVERE, messages);
            throw new Exception();
        }
    }

    private void loadHashAndString(JMorfSdk jMorfSdk, InputStream inputStreamHashCodeAndMorfCharacteristics, Scanner scannerInitialFormString) {

        HashMap<Integer, String> initialFormString = loadInitialFormString(scannerInitialFormString);

        byte[] bytesFile;
        try {
            bytesFile = sun.misc.IOUtils.readFully(inputStreamHashCodeAndMorfCharacteristics, sizeFileHashAndMorfCharacteristics, true);

            while (pointer < bytesFile.length) {

                int nextHashCode = getHashCodeFromBytes(bytesFile);
                String strWordform = initialFormString.get(nextHashCode);
                byte typeOfSpeech = getTypeOfSpeechFromBytes(bytesFile);
                long morfCharacteristics = getMorfCharacteristicsFromBytes(bytesFile);

                InitialForm initialForm = new InitialForm(strWordform, typeOfSpeech, morfCharacteristics);
                jMorfSdk.addInitialForm(initialForm);
                strWordform = null;

                nextHashCode = getHashCodeFromBytes(bytesFile);
                while (nextHashCode != CONTROLVALUE) {
                    morfCharacteristics = getMorfCharacteristicsFromBytes(bytesFile);
                    jMorfSdk.addWordForm(nextHashCode, new WordForm(morfCharacteristics, initialForm));
                    nextHashCode = getHashCodeFromBytes(bytesFile);
                }

                pointer += 4;
            }

        } catch (IOException ex) {
            Logger.getLogger(LoadBasedOnHashCodeFromZip.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            bytesFile = null;
            initialFormString.clear();
            initialFormString = null;
        }
    }

    private HashMap<Integer, String> loadInitialFormString(Scanner readerFormHashAndString) {
        HashMap<Integer, String> initialForm = new HashMap<>();
        while(readerFormHashAndString.hasNext()) {
            String formString = readerFormHashAndString.nextLine();
            initialForm.put(formString.hashCode(), formString);
        }
        return initialForm;
    }

    private int getHashCodeFromBytes(byte[] bytesFile) {
        return (int) getValueCodeFromBytes(bytesFile, 4);
    }

    private byte getTypeOfSpeechFromBytes(byte[] bytesFile) {
        byte typeOfSpeech = bytesFile[pointer];
        pointer++;
        return typeOfSpeech;
    }

    private long getMorfCharacteristicsFromBytes(byte[] bytesFile) {
        return getValueCodeFromBytes(bytesFile, 8);
    }

    private long getValueCodeFromBytes(byte[] bytesFile, int countByte) {
        int hashCode = 0;
        for (int i = 0; i < countByte; i++) {
            int f = 0xFF & bytesFile[pointer + countByte - i - 1];
            int g1 = f << (8 * i);
            hashCode |= g1;
        }
        pointer += countByte;
        return hashCode;
    }

    private void closeStream(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException ex) {
            Logger.getLogger(LoadBasedOnHashCodeFromZip.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            inputStream = null;
        }
    }

    private void closeScanner(Scanner scanner) {
        scanner.close();
        scanner = null;
    }

    @Override
    public JMorfSdk loadLibraryForSearchForFormByMorphologicalCharacteristics() {
        JMorfSdk jMorfSdk = loadLibraryForSearchInitialForm();
        loadWordFormsString(jMorfSdk);
        return jMorfSdk;
    }

    private void loadWordFormsString(JMorfSdk jMorfSdk) {

        Scanner scannerWordFormString = null;

        try {
            scannerWordFormString = openScannerFromFile(Property.pathWordFormString, Property.encoding);
            loadWordString(jMorfSdk, scannerWordFormString);
        } catch (Exception ex) {
            Logger.getLogger(LoadBasedOnHashCodeFromZip.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeScanner(scannerWordFormString);
        }
    }

    private Scanner openScannerFromFile(String pathLibrary, String encoding) throws Exception {
        try {
            return new Scanner(new InputStreamReader(new FileInputStream(pathLibrary), encoding));
        } catch (FileNotFoundException ex) {
            String messages = String.format("Ошибка при чтении файла.\r\nПроверте наличие %s, в случае отсуствия скачайте с репозитория %s\r\n", pathLibrary, MYREPOSITORY);
            Logger.getLogger(LoadBasedOnHashCode.class.getName()).log(Level.SEVERE, messages);
            throw new Exception();
        }
    }

    private void loadWordString(JMorfSdk jMorfSdk, Scanner scannerWordFormString) {
        while(scannerWordFormString.hasNext()) {
            String formString = scannerWordFormString.nextLine();
            jMorfSdk.addStringForm(formString);
        }
    }
}