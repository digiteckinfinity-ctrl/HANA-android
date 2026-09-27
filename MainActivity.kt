package com.hana.korean

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

data class Lesson(val ko:String,val roman:String,val en:String,val ta:String,val vocab:String)

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var ko: TextView; private lateinit var roman: TextView
    private lateinit var en: TextView; private lateinit var ta: TextView
    private lateinit var vocab: TextView; private lateinit var transcript: TextView
    private lateinit var correction: TextView; private lateinit var send: Button
    private var heard = ""; private var index = 0

    private val lessons = listOf(
        Lesson("안녕하세요! 오늘 기분이 어때요?","an-nyeong-ha-se-yo! o-neul gi-bun-i eo-ttae-yo?",
            "Hello! How do you feel today?","வணக்கம்! இன்று எப்படி உணர்கிறீர்கள்?",
            "안녕하세요 (an-nyeong-ha-se-yo) — hello — வணக்கம்\n오늘 (o-neul) — today — இன்று\n기분 (gi-bun) — mood — மனநிலை\n어때요? (eo-ttae-yo?) — how is it? — எப்படி இருக்கிறது?"),
        Lesson("오늘 뭐 했어요?","o-neul mwo hae-sseo-yo?","What did you do today?","இன்று என்ன செய்தீர்கள்?",
            "오늘 (o-neul) — today — இன்று\n뭐 (mwo) — what — என்ன\n했어요 (hae-sseo-yo) — did — செய்தீர்கள்"),
        Lesson("좋아하는 음식이 뭐예요?","jo-a-ha-neun eum-sik-i mwo-ye-yo?","What food do you like?","உங்களுக்கு பிடித்த உணவு என்ன?",
            "좋아하다 (jo-a-ha-da) — to like — பிடிக்கும்\n음식 (eum-sik) — food — உணவு\n뭐예요? (mwo-ye-yo?) — what is it? — என்ன?")
    )

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if(ok) startSpeech() else transcript.text = "Microphone permission is required."
    }
    private val speech = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val list = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        heard = list?.firstOrNull().orEmpty()
        transcript.text = if(heard.isBlank()) "I couldn't hear that. Tap Record and try again." else "You said: $heard"
        send.isEnabled = heard.isNotBlank()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_main)
        tts = TextToSpeech(this,this)
        ko=findViewById(R.id.korean); roman=findViewById(R.id.pronunciation); en=findViewById(R.id.english)
        ta=findViewById(R.id.tamil); vocab=findViewById(R.id.vocab); transcript=findViewById(R.id.transcript)
        correction=findViewById(R.id.correction); send=findViewById(R.id.send)
        findViewById<Button>(R.id.record).setOnClickListener {
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) startSpeech()
            else permission.launch(Manifest.permission.RECORD_AUDIO)
        }
        findViewById<Button>(R.id.speaker).setOnClickListener { speakCurrent() }
        send.setOnClickListener { handleSend() }
        findViewById<Button>(R.id.next).setOnClickListener { index=(index+1)%lessons.size; showLesson(); correction.text="" }
        showLesson()
    }

    private fun startSpeech(){
        val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR")
            putExtra(RecognizerIntent.EXTRA_PROMPT,"한국어로 말해 보세요")
        }
        try { speech.launch(i) } catch(e:Exception) { transcript.text="Speech recognition is not available on this device." }
    }
    private fun handleSend(){
        correction.text = if(heard.contains("안녕")) "✓ Good Korean! 안녕하세요 is the polite form of hello."
        else "Practice note: Your speech was received. Repeat the model sentence and compare the pronunciation."
        index=(index+1)%lessons.size; showLesson(); heard=""; send.isEnabled=false
    }
    private fun showLesson(){
        val x=lessons[index]; ko.text=x.ko; roman.text=x.roman; en.text="English: ${x.en}"
        ta.text="Tamil: ${x.ta}"; vocab.text="Vocabulary\n${x.vocab}"
    }
    private fun speakCurrent(){ tts.language=Locale.KOREAN; tts.speak(lessons[index].ko,TextToSpeech.QUEUE_FLUSH,null,"hana") }
    override fun onInit(status:Int){ if(status==TextToSpeech.SUCCESS) tts.language=Locale.KOREAN }
    override fun onDestroy(){ tts.stop(); tts.shutdown(); super.onDestroy() }
}