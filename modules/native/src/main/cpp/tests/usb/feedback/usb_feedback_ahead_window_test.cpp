#include "usb/feedback/usb_feedback_ahead_window.h"

#include <cassert>
#include <cstdint>
#include <limits>

namespace {

using memi::usb::feedback::FeedbackAheadWindowInput;
using memi::usb::feedback::FeedbackAheadWindowStatus;

void acceptsTransferAtExactWindowBoundary() {
    const auto result = memi::usb::feedback::checkFeedbackAheadWindow(
        FeedbackAheadWindowInput { 24, 16, 8, 16 }
    );
    assert(result.status == FeedbackAheadWindowStatus::Ok);
    assert(result.currentAheadIntervals == 8);
    assert(result.projectedAheadIntervals == 16);
    assert(result.remainingIntervals == 0);
}

void rejectsCounterOrderAndWindowOverrun() {
    auto result = memi::usb::feedback::checkFeedbackAheadWindow(
        FeedbackAheadWindowInput { 10, 11, 1, 16 }
    );
    assert(result.status == FeedbackAheadWindowStatus::CounterOrder);

    result = memi::usb::feedback::checkFeedbackAheadWindow(
        FeedbackAheadWindowInput { 24, 16, 9, 16 }
    );
    assert(result.status == FeedbackAheadWindowStatus::CapacityExceeded);
    assert(result.remainingIntervals == 8);
}

void rejectsOverflowWithoutWrappingCounters() {
    const auto result = memi::usb::feedback::checkFeedbackAheadWindow(
        FeedbackAheadWindowInput {
            std::numeric_limits<uint64_t>::max() - 1U,
            0,
            2,
            std::numeric_limits<uint64_t>::max()
        }
    );
    assert(result.status == FeedbackAheadWindowStatus::Overflow);
    assert(result.currentAheadIntervals ==
        std::numeric_limits<uint64_t>::max() - 1U);
}

void rejectsZeroSizedConfiguration() {
    const auto result = memi::usb::feedback::checkFeedbackAheadWindow(
        FeedbackAheadWindowInput { 0, 0, 0, 16 }
    );
    assert(result.status == FeedbackAheadWindowStatus::InvalidArgument);
}

} // namespace

int main() {
    acceptsTransferAtExactWindowBoundary();
    rejectsCounterOrderAndWindowOverrun();
    rejectsOverflowWithoutWrappingCounters();
    rejectsZeroSizedConfiguration();
    return 0;
}
