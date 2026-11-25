package com.mmc.merimusicplayer.data.ltw

import com.mmc.merimusicplayer.data.ltw.invite.parseListenTogetherInvite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenTogetherBaseUrlIntegrationTest {

    @Test
    fun `invite parser normalizes valid custom server`() {
        val invite = parseListenTogetherInvite(
            "memiplayer://listen-together/join?roomId=P8BAEV&baseUrl=https%3A%2F%2Fexample.com%2F&secret=secret-value"
        )

        assertEquals("https://example.com", invite?.baseUrl)
        assertFalse(invite?.hasInvalidBaseUrl ?: true)
    }

    @Test
    fun `invite parser keeps encoded worker base url from shared text`() {
        val invite = parseListenTogetherInvite(
            "Memi5458C5 邀请你在 MemiPlayer 中加入房间 GTV42X。复制后打开 App 加入。\n" +
                "memiplayer://listen-together/join?roomId=GTV42X&inviter=Memi5458C5&baseUrl=https%3A%2F%2Fmemiplayerltw.cwuomcwuom.workers.dev&secret=secret-value"
        )

        assertEquals("GTV42X", invite?.roomId)
        assertEquals("Memi5458C5", invite?.inviterNickname)
        assertEquals("https://memiplayerltw.cwuomcwuom.workers.dev", invite?.baseUrl)
        assertFalse(invite?.hasInvalidBaseUrl ?: true)
    }

    @Test
    fun `invite parser carries room join secret`() {
        val invite = parseListenTogetherInvite(
            "memiplayer://listen-together/join?roomId=GTV42X&secret=secret-value"
        )

        assertEquals("secret-value", invite?.joinSecret)
    }

    @Test
    fun `invite parser rejects missing room join secret`() {
        assertNull(
            parseListenTogetherInvite("memiplayer://listen-together/join?roomId=GTV42X")
        )
    }

    @Test
    fun `clipboard text without an invitation cannot be parsed for a room join`() {
        assertNull(parseListenTogetherInvite(null as String?))
        assertNull(
            parseListenTogetherInvite("roomId=GTV42X&secret=secret-value")
        )
    }

    @Test
    fun `invite parser decodes encoded inviter`() {
        val invite = parseListenTogetherInvite(
            "memiplayer://listen-together/join?roomId=P8BAEV&inviter=Memi%E7%8C%AB&secret=secret-value"
        )

        assertEquals("Memi猫", invite?.inviterNickname)
    }

    @Test
    fun `invite parser rejects malformed query encoding without throwing`() {
        assertNull(
            parseListenTogetherInvite("memiplayer://listen-together/join?roomId=%")
        )
    }

    @Test
    fun `invite parser flags invalid custom server`() {
        val invite = parseListenTogetherInvite(
            "memiplayer://listen-together/join?roomId=P8BAEV&baseUrl=example.com&secret=secret-value"
        )

        assertNull(invite?.baseUrl)
        assertTrue(invite?.hasInvalidBaseUrl == true)
    }

    @Test
    fun `invite parser rejects cleartext http server`() {
        val invite = parseListenTogetherInvite(
            "memiplayer://listen-together/join?roomId=P8BAEV&baseUrl=http%3A%2F%2F192.168.1.10%3A8787%2F&secret=secret-value"
        )

        assertNull(invite?.baseUrl)
        assertTrue(invite?.hasInvalidBaseUrl == true)
    }
}
