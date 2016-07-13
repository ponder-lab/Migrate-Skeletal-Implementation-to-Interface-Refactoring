package p;

interface I {
	default I m() {
		return this;
	}
}