# JMorfSdk<br>
<b>JMorfSdk</b> - главный класс, содержит <b>HashMap</b> словоформ и <b>HashMap</b> всех начальных форм слова.<br>
<br>
<b>OmoForm</b> - омоформы слова, класс наследуется от <b>ArrayList</b> накаливает в себе все формы (<b>Form</b>) данного слова, например, мыла<br>
<b>Form</b> - форма слова содержит в себе набор характеристик, имеет набор характеристик, которые меняются в зависимости от словоформы (число, падеж и т.д.)<br>
<b>WordForm</b> - словоформа, наследуется от <b>Form</b>, имеет ссылку на <b>MainForm</b>.<br>
<b>MainForm</b> наследуется от <b>Form</b> - словоформа в начальной форме слова, имеет ссылки на все производные словоформы (<b>WordfForm</b>) и имеет постоянные характеристики (часть речи, род).<br>
<b>MorfologyCharacteristics</b> - содержит характеристики, которые меются в зависимости от словоформы.<br>
<b>TypeOfSpeech</b> - набор констант для частей речи, например, Noun = 1 и т.д.<br>
